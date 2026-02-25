package mp3;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javazoom.jl.player.Player;
import javazoom.jl.decoder.JavaLayerException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Refactored version:
 * - Removed Runnable + manual Thread control (stop/suspend/resume).
 * - Uses ExecutorService + Future cancellation + stream close (cooperative cancellation).
 * - UI updates are executed on Swing EDT.
 * - "Pause" is implemented as safe Stop (restart from beginning on next Play).
 */

public class MP3_Player extends JFrame implements ActionListener, MouseListener, KeyListener {
    private static final int EXIT_CODE = 0;

    // ===== 기존 코드 =====
//    private static Player player;
//    private Thread musicThread;
//    private FileInputStream fileInputStream;
//    private boolean isPlaying = false; //재생 중인지 아닌지 판단하는 변수

    // ===== 변경 코드 =====
    private final ExecutorService playbackExec = newSingleThreadExecutor();
    private Future<?> playbackJob;

    private volatile Player currentPlayer;
    private volatile FileInputStream currentStream;

    private volatile boolean isPlaying = false; //재생 중인지 아닌지 판단하는 변수

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    //index: 플레이 리스트의 인덱스 번호를 결정하는 변수
    //mode: 반복 없음, 한곡 반복, 전체 반복을 결정하는 변수
    private int index, mode = 0;

    private JMenuItem openMenuItem, exitMenuItem, infoMenuItem;

    private JLabel titleLabel, subTitleLabel, playListLabel; //제목, 부제목, 재생목록 레이블
    private JButton prevButton, playPauseButton, stopButton, nextButton, repeatButton; //5개의 버튼

    private ArrayList<File> fileList = new ArrayList<>();
    private JList<String> playList; //playList
    private boolean doubleClicked = false;

    //5개의 버튼
    private final ImageIcon prevIcon = new ImageIcon("src/images/prev.png"); //이전 버튼
    private final ImageIcon playIcon = new ImageIcon("src/images/play.png"); //재생 버튼
    private final ImageIcon pauseIcon = new ImageIcon("src/images/pause.png"); //일시 정지 버튼
    private final ImageIcon stopIcon = new ImageIcon("src/images/stop.png"); //정지 버튼
    private final ImageIcon nextIcon = new ImageIcon("src/images/next.png"); //다음 버튼

    private File currentFile;
    
    private volatile boolean playTriggeredByDoubleClick = false;

    public MP3_Player(String subject, int width, int height) {
        setTitle(subject);
        setSize(width, height);
        setLocation(800, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializeComponents();
        setupLayout();
        
        setVisible(true);
    }

    // 컴포넌트 초기화하는 함수
    private void initializeComponents() {
        Font Gullim = new Font("굴림", Font.PLAIN, 15);
        Font gungseo = new Font("궁서", Font.PLAIN, 20);
        Font dotum = new Font("돋움", Font.PLAIN, 15);
        Font batang = new Font("바탕", Font.PLAIN, 15);
        Font gothic = new Font("맑은 고딕", Font.PLAIN, 12);

        //메뉴바
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(236, 209, 116));
        
        JMenu fileMenu = new JMenu("파일");
        fileMenu.setFont(Gullim);

        openMenuItem = new JMenuItem("열기");
        openMenuItem.setFont(Gullim);
        openMenuItem.setToolTipText("새 음악 파일을 열거나 추가합니다.");
        openMenuItem.addActionListener(this);

        exitMenuItem = new JMenuItem("종료");
        exitMenuItem.setFont(Gullim);
        exitMenuItem.setToolTipText("프로그램을 종료합니다.");
        exitMenuItem.addActionListener(this);

        JMenu infoMenu = new JMenu("정보");
        infoMenu.setFont(Gullim);

        infoMenuItem = new JMenuItem("프로그램 정보");
        infoMenuItem.setFont(Gullim);
        infoMenuItem.setToolTipText("프로그램의 정보입니다.");
        infoMenuItem.addActionListener(this);

        fileMenu.add(openMenuItem);
        fileMenu.add(exitMenuItem);
        infoMenu.add(infoMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(infoMenu);
        setJMenuBar(menuBar);

        //레이블
        titleLabel = new JLabel("Seong Eun Song");
        titleLabel.setFont(dotum);

        subTitleLabel = new JLabel("음악 파일을 추가하려면 파일 - 열기 버튼을 누르세요.");
        subTitleLabel.setFont(batang);

        playListLabel = new JLabel("재생 목록"); //제목은 "재생 목록"
        playListLabel.setFont(dotum);

        // 버튼
        prevButton = createButton(prevIcon, "이전 트랙을 재생합니다.", gothic); //이전 트랙 버튼
        playPauseButton = createButton(playIcon, "음악 파일을 재생합니다.", gothic); //재생 버튼
        stopButton = createButton(stopIcon, "음악 재생을 정지합니다.", gothic); //정지 버튼
        nextButton = createButton(nextIcon, "다음 트랙을 재생합니다.", gothic); //다음 트랙 버튼

        repeatButton = new JButton("반복 없음"); //반복 없음, 한곡 반복, 전체 반복 순환 버튼
        repeatButton.setToolTipText("반복 설정");
        repeatButton.setFont(gothic);
        repeatButton.addActionListener(this);

        //재생 목록
        playList = new JList<String>(new DefaultListModel<String>());
        playList.setFont(dotum);
        playList.addMouseListener(this);
        playList.addKeyListener(this);
    }

    //버튼을 만드는 함수
    private JButton createButton(ImageIcon icon, String tooltip, Font font) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFont(font);
        button.addActionListener(this);
        return button;
    }

    // 레이아웃
    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(4, 1));

        JPanel titlePanel = new JPanel();
        JPanel subTitlePanel = new JPanel();

        JPanel buttonPanel = new JPanel(new GridLayout(1, 5));
        buttonPanel.add(prevButton);
        buttonPanel.add(playPauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(repeatButton);

        JPanel playListPanel = new JPanel();

        titlePanel.setBackground(new Color(186, 255, 26));
        subTitlePanel.setBackground(Color.white);
        playListPanel.setBackground(new Color(255, 149, 102));
        playList.setBackground(new Color(144, 245, 255));

        titlePanel.add(titleLabel);
        subTitlePanel.add(subTitleLabel);
        playListPanel.add(playListLabel);

        topPanel.add(titlePanel, 0);
        topPanel.add(subTitlePanel);
        topPanel.add(buttonPanel);
        topPanel.add(playListPanel);

        JScrollPane scrollPane = new JScrollPane(playList);
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ===== 이벤트 핸들링 =====
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource(); // 객체 내용 받아오기

        if (source == openMenuItem) { // 메뉴 아이템 중에 열기 버튼을 눌렀을 때
            openFiles(); // 파일 열기
            return;
        }
        
        if (source == exitMenuItem) { // 메뉴 아이템 중에 종료 버튼을 눌렀을 때
            if (confirmExit()) {
                shutdownPlayback();
                System.exit(EXIT_CODE);
            }
            return;
        }

        if (source == infoMenuItem) { // 메뉴 아이템 중에 정보 버튼을 눌렀을 때
            showInfoDialog();
            return;
        }

        if (source == prevButton) { // 이전 버튼이 눌렸을 때
            playPreviousTrack();
            return;
        }

        if (source == playPauseButton) { //재생_일시정지 버튼이 눌렸을 때
            togglePlayPause();
            return;
        }

        if(source == stopButton) { //정지 버튼이 눌렸을 때
            stopMusic();
            return;
        }

        if(source == nextButton) { //다음 버튼이 눌렸을 때
            playNextTrackManually();
            return;
        }

        if (source == repeatButton) { //반복 버튼이 눌렸을 때
            cycleRepeatMode();
        }
    }

    // ===== Playback Control (refactored) =====

    // 재생-일시정지 버튼이 눌렸을 때 실행되는 메소드
    private void togglePlayPause() {
        if(!isPlaying) {//음악이 재생 중이 아닐 때
            if(fileList.isEmpty()) { //리스트에 파일이 없을 때
                showNoFileError();
                return;
            }
            // 만약 아무것도 선택되지 않는다면 범위 내의 인덱스 유지
            if (index < 0 || index >= fileList.size()) index = 0;

            startMusic(fileList.get(index)); //파일 리스트의 인덱스 번째의 음악을 재생
            return;
        }

        // "Pause" -> safe stop (restart from beginning)
        SwingUtilities.invokeLater(() -> {
            titleLabel.setText("음악 일시 정지됨");
            subTitleLabel.setText("재생 버튼을 누르면 처음부터 재생됩니다.");
            playPauseButton.setIcon(playIcon);
        });

        stopMusicInternal();
        isPlaying = false;
    }

    // 정지 버튼이 눌렸을 때 실행되는 메소드
    private void stopMusic() {
        if (fileList.isEmpty()) { // 파일 리스트가 비었는지 확인하고 비었으면 파일 없음 오류 메시지 출력
            showNoFileError();
            return;
        }

        stopMusicInternal();

        SwingUtilities.invokeLater(() -> {
            titleLabel.setText("Seong Eun Song"); //제목을 "Seong Eun Song"으로 변경
            subTitleLabel.setText("음악 정지");
            playPauseButton.setIcon(playIcon); //아이콘을 재생 아이콘으로 변경
        });

        isPlaying = false; //파워 꺼짐
    }

    public void startMusic(File file) {
        // ===== 기존 재생 중이면 정지 =====
        stopMusicInternal();

        currentFile = file;

        SwingUtilities.invokeLater(() -> {
            titleLabel.setText("현재 재생 중인 파일은 " + currentFile.getName() + " 입니다."); //재생 중의 음악의 제목을 표시
            subTitleLabel.setText("음악 재생 중");
            playPauseButton.setIcon(pauseIcon);
        });

        playbackJob = playbackExec.submit(() -> {
            stopRequested.set(false);

            try (FileInputStream fis = new FileInputStream(currentFile)) {
                currentStream = fis;
                currentPlayer = new Player(fis);
                isPlaying = true;

                // Blocking play (runs in background thread)
                currentPlayer.play();

                SwingUtilities.invokeLater(this::onTrackCompleted);
            } catch (Exception ex) {
                // ✅ 취소/정지/곡전환으로 인한 예외면 팝업 띄우지 않음
                if (stopRequested.get() || Thread.currentThread().isInterrupted()) {
                    return;
                }
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "재생 오류: " + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE)
                );
            } finally {
                isPlaying = false;
                currentPlayer = null;
                currentStream = null;
            }
        });
    }

    private void onTrackCompleted() {
        // If user triggered another track via double-click/manual next, this completion might be from old job.
        // Since stopMusicInternal() cancels & closes stream, completion after that is unlikely, but we keep logic simple.

        // mode based auto-next
        if(fileList.isEmpty()) {
            resetToIdle("Seong Eun Song", "재생할 파일이 없습니다.");
            return;
        }

        try {
            playNextTrackAuto(playTriggeredByDoubleClick);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "다음 곡 재생 중 오류: " + e.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        } finally {
            playTriggeredByDoubleClick = false;
        }
    }

    private void stopMusicInternal() {
        stopRequested.set(true);
        // ===== 기존 스레드 취소 =====
        if (playbackJob != null) {
            playbackJob.cancel(true);
            playbackJob = null;
        }

        // ===== 스트림 닫아서 재생 종료 =====
        try {
            if (currentStream != null) currentStream.close();
        } catch (Exception ignored) {}

        currentPlayer = null;
        currentStream = null;
    }

    private void shutdownPlayback() {
        stopMusicInternal();
        playbackExec.shutdownNow();
    }

    private void resetToIdle(String title, String subtitle) {
        SwingUtilities.invokeLater(() -> {
            titleLabel.setText(title);
            subTitleLabel.setText(subtitle);
            playPauseButton.setIcon(playIcon);
        });
        isPlaying = false;
    }

    // ===== Track Navigation =====

    // 이전 버튼이 눌렸을 때 실행되는 메소드
    private void playPreviousTrack() {
        if (fileList.isEmpty()) { //리스트에 파일이 없을 때
            showNoFileError();
            return;
        }
        index = (index - 1 + fileList.size()) % fileList.size(); //인덱스 감소 - 이전 트랙 음악 선택, 0일 때의 경우 생각
        stopMusicInternal(); //내부 음악 중지
        startMusic(fileList.get(index)); //파일 리스트의 인덱스 번째의 음악, 이전 트랙 재생
    }

    private void playNextTrackManually() {
        if (fileList.isEmpty()) {
            showNoFileError();
            return;
        }
        index++;
        if (index >= fileList.size()) index = 0;

        stopMusicInternal();
        startMusic(fileList.get(index));
    }

    /**
     * Auto-next logic based on repeat mode.
     * @param triggeredByDoubleClick kept for compatibility with your original intent; now simplified.
     */
    private void playNextTrackAuto(boolean triggeredByDoubleClick) {
        // NOTE: triggeredByDoubleClick no longer changes logic much; we keep behavior similar.
        // You can remove this param entirely after confirming expected behavior.

        if (mode == 0) { // no repeat
            if (!triggeredByDoubleClick) index++;

            if (index < fileList.size()) {
                startMusic(fileList.get(index));
            } else {
                // reached end
                index = 0;
                resetToIdle("Seong Eun Song", "리스트에 있는 모든 음악 재생 끝");
            }
            return;
        }

        if (mode == 1) { // repeat one
            startMusic(fileList.get(index));
            return;
        }

        // mode == 2 repeat all
        if (!triggeredByDoubleClick) index++;
        if (index >= fileList.size()) index = 0;
        startMusic(fileList.get(index));
    }

    private void cycleRepeatMode() {
        mode++;
        if(mode >= 3) { //모드가 3이 되면
            mode = 0; //모드 0으로 다시 돌리기
        }
        switch(mode) { //모드
            case 0 -> repeatButton.setText("반복 없음"); //무반복 모드
            case 1 -> repeatButton.setText("한곡 반복"); //한번 반복 모드
            case 2 -> repeatButton.setText("전체 반복"); //전체 반복 모드
        }
    }

    // ===== UI Helpers =====

    private void showInfoDialog() {
        MII_Dialog dialog = new MII_Dialog("프로그램 정보", 250, 240);//다이얼로그 창 띄우기
        dialog.setVisible(true);
    }

    private boolean confirmExit() {
        int result = JOptionPane.showConfirmDialog(this, "정말 종료하시겠습니까?", "종료",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    private void showNoFileError() {
        JOptionPane.showMessageDialog(this, "리스트에 파일이 존재하지 않습니다!",
                "오류", JOptionPane.ERROR_MESSAGE);
    }

    // ===== File Open (kept: JavaFX FileChooser) =====
    private void openFiles() {
        DefaultListModel<String> model = (DefaultListModel<String>) playList.getModel();

        Platform.runLater(() -> {
            // 윈도우 파일 선택기 인스턴스 생성
            FileChooser fileChooser = new FileChooser();

            // 파일 선택기 창 제목
            fileChooser.setTitle("mp3 파일 선택하기");

            // MP3 파일만 표시하도록 파일 확장자 필터 설정
            FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("음악 파일(*.mp3)", "*.mp3");
            fileChooser.getExtensionFilters().add(filter);

            // 다중 선택 기능
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(new Stage());
            if(selectedFiles == null) return;

            // NOTE: This code is running on JavaFX Application Thread.
            // We must update Swing UI on EDT.
            SwingUtilities.invokeLater(() -> {
                for (File file : selectedFiles) { //파일 배열의 길이 만큼 i를 증가
                    if (file == null) continue;

                    if (fileList.contains(file)) { //배열에 저장된 파일이 중복될 때
                        JOptionPane.showMessageDialog(null, "이미 존재하는 파일입니다.",
                                "알림", JOptionPane.INFORMATION_MESSAGE);
                        continue;
                    }

                    if (!file.getName().toLowerCase().endsWith("mp3")) {//확장자가 mp3가 아닐 때
                        JOptionPane.showMessageDialog(null, "파일 형식이 올바르지 않습니다.\n"
                                        + "이 플레이어는 확장자가 mp3인 파일만 지원합니다.",
                                "경고", JOptionPane.WARNING_MESSAGE);
                        continue;
                    }

                    fileList.add(file); // 파일 리스트에 음악 파일 추가
                    model.addElement(file.getName()); // 재생 목록 UI에 음악 파일 추가

                    if (!isPlaying) {
                        subTitleLabel.setText("음악 파일을 재생하려면 재생 버튼을 누르세요.");
                    }
                }
            });
        });
    }

    // ===== Playlist delete =====
    private void deleteTrack(int[] selectedIndices) {
        DefaultListModel<String> model = (DefaultListModel<String>) playList.getModel();

        // 역순으로 삭제
        for(int j = selectedIndices.length - 1; j >= 0; j--) {
            int removeIndex = selectedIndices[j];
            if(removeIndex < 0 || removeIndex >= fileList.size()) {
                continue;
            }
            fileList.remove(selectedIndices[j]); //i 배열의 인덱스 j의 파일을 지운다. 없으면 파일은 남아있는데 이름만 지워지고 인덱스가 재설정이 안된다.
            model.remove(selectedIndices[j]); //재생 목록에서 파일 이름을 지운다. 없으면 이름이 안 지워진다.

            // Adjust current index if needed
            if (removeIndex <= index && index > 0) {
                index--;
            }
        }

        // 인덱스 보정
        if (fileList.isEmpty()) index = 0;
        else if (index >= fileList.size()) index = fileList.size() - 1;

        //갱신
        playList.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 1) {
            index = playList.locationToIndex(e.getPoint());
            return;
        }

        // Double click to play
        if (playList == e.getComponent() && e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            int clicked = playList.locationToIndex(e.getPoint());
            if (clicked < 0) return;

            index = clicked;

            if(!fileList.isEmpty() && index >= 0 && index < fileList.size()) {
                playTriggeredByDoubleClick = true;
                stopMusicInternal();
                startMusic(fileList.get(index));
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_DELETE) { //눌린 키가 delete 키라면
            deleteTrack(playList.getSelectedIndices()); //선택된 것들의 트랙 지우기 (다중 선택 지우기)
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
