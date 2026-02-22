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

public class MP3_Player extends JFrame implements ActionListener, MouseListener, KeyListener, Runnable {
    private static final int EXIT_CODE = 0;
    private static Player player;

    //index: 플레이 리스트의 인덱스 번호를 결정하는 변수
    //mode: 반복 없음, 한곡 반복, 전체 반복을 결정하는 변수
    private int index, mode = 0;
    private boolean isPlaying = false; //재생 중인지 아닌지 판단하는 변수
    private Thread musicThread;

    private JMenuItem openMenuItem, exitMenuItem, infoMenuItem;

    private JLabel titleLabel, subTitleLabel, playListLabel; //제목, 부제목, 재생목록 레이블
    private JButton prevButton, playPauseButton, stopButton, nextButton, repeatButton; //5개의 버튼

    private static ArrayList<File> fileList = new ArrayList<File>();
    private static JList<String> playList; //playList
    private boolean doubleClicked = false;

    //5개의 버튼
    private final ImageIcon prevIcon = new ImageIcon("src/images/prev.png"); //이전 버튼
    private final ImageIcon playIcon = new ImageIcon("src/images/play.png"); //재생 버튼
    private final ImageIcon pauseIcon = new ImageIcon("src/images/pause.png"); //일시 정지 버튼
    private final ImageIcon stopIcon = new ImageIcon("src/images/stop.png"); //정지 버튼
    private final ImageIcon nextIcon = new ImageIcon("src/images/next.png"); //다음 버튼

//    private static JList<String> jList;
    private File currentFile;
    private FileInputStream fileInputStream;

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

    @SuppressWarnings({"removal"})
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource(); // 객체 내용 받아오기
        if (source == openMenuItem) { // 메뉴 아이템 중에 열기 버튼을 눌렀을 때
            openFiles(); // 파일 열기
        } else if (source == exitMenuItem) { // 메뉴 아이템 중에 종료 버튼을 눌렀을 때
            if (confirmExit()) System.exit(EXIT_CODE);
        } else if (source == infoMenuItem) { // 메뉴 아이템 중에 정보 버튼을 눌렀을 때
            showInfoDialog();
        } else if (source == prevButton) { // 이전 버튼이 눌렸을 때
            try {
                playPreviousTrack();
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } else if (source == playPauseButton) { //재생_일시정지 버튼이 눌렸을 때
            try {
                togglePlayPause();
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } else if(source == stopButton) { //정지 버튼이 눌렸을 때
            stopMusic();
        } else if(source == nextButton) { //다음 버튼이 눌렸을 때
            if(!fileList.isEmpty()) { //리스트에 파일이 있을 때
                if(musicThread.isAlive()) { //뮤직 스레드가 살아 있다면
                    musicThread.stop();	//뮤직 스레드를 정지
                    //스레드를 정지하는 이유: 노래가 중복 재생이 된다. 이전 스레드를 닫고, 새로운 스레드를 생성한다.
                }
                try {
                    playNextTrack(); //다음 트랙 재생
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else { //리스트에 파일이 없을 때
                JOptionPane.showMessageDialog(null, "리스트에 파일이 존재하지 않습니다!",
                        "오류", JOptionPane.ERROR_MESSAGE); //오류 메시지 띄우기
            }

        } else if (source == repeatButton) { //반복 버튼이 눌렸을 때
            mode++;
            if(mode >= 3) { //모드가 3이 되면
                mode = 0; //모드 0으로 다시 돌리기
            }
            switch(mode) { //모드
                case 0: //무반복 모드
                    repeatButton.setText("반복 없음");
                    break;
                case 1: //한번 반복 모드
                    repeatButton.setText("한곡 반복");
                    break;
                case 2: //전체 반복 모드
                    repeatButton.setText("전체 반복");
                    break;
            }
        }
    }

    // 정지 버튼이 눌렸을 때 실행되는 메소드
    @SuppressWarnings("removal")
    private void stopMusic() {
        if(!fileList.isEmpty()) {
            musicThread.stop(); //음악 스레드를 정지한다.
            titleLabel.setText("Seong Eun Song"); //제목을 "Seong Eun Song"으로 변경
            subTitleLabel.setText("음악 정지");
            if(playPauseButton.getIcon() == pauseIcon) { //재생_일시정지 버튼의 아이콘이 일시정지 아이콘이었다면
                playPauseButton.setIcon(playIcon); //아이콘을 재생 아이콘으로 변경
            }
            isPlaying = false; //파워 꺼짐
        }
        else {
            JOptionPane.showMessageDialog(null, "리스트에 파일이 존재하지 않습니다!",
                    "오류", JOptionPane.ERROR_MESSAGE); //오류 메시지 띄우기
        }
    }

    // 재생-일시정지 버튼이 눌렸을 때 실행되는 메소드
    @SuppressWarnings("removal")
    private void togglePlayPause() throws FileNotFoundException {
        if(!isPlaying) { //음악이 재생 중이 아닐 때
            if(!fileList.isEmpty()) { //리스트에 파일이 있을 때
                startMusic(fileList.get(index)); //파일 리스트의 인덱스 번째의 음악을 재생
                playPauseButton.setIcon(pauseIcon); //일시정지 버튼으로 변경
            } else { //리스트에 파일이 없을 때
                JOptionPane.showMessageDialog(null, "리스트에 파일이 존재하지 않습니다!",
                        "오류", JOptionPane.ERROR_MESSAGE); //오류 메시지 띄우기
            }
        } else { //음악이 재생 중일 때
            if (playPauseButton.getIcon() == pauseIcon) { //재생_일시정지 버튼의 아이콘이 일시정지 아이콘일 때
                titleLabel.setText("음악 일시 정지됨"); //제목을 "일시 정지"로 변경
                subTitleLabel.setText("재생 버튼을 누르면 다시 재생됩니다."); //부제목 변경
                playPauseButton.setIcon(playIcon); //재생_일시정지 버튼의 아이콘을 재생 아이콘으로 변경
                musicThread.suspend(); //음악 스레드를 일시정지한다. (지원 중단된 메소드)
            } else if (playPauseButton.getIcon() == playIcon) { //재생_일시정지 버튼의 아이콘이 재생 아이콘일 때
                titleLabel.setText("현재 재생 중인 파일은 " + currentFile.getName() + " 입니다."); //제목을 다시 되돌린다.
                subTitleLabel.setText("음악 재생 중"); //부제목 변경
                playPauseButton.setIcon(pauseIcon); //재생_일시정지 버튼의 아이콘을 일시정지 아이콘으로 변경
                musicThread.resume(); //음악 스레드의 일시 정지를 푼다. (지원 중단된 메소드)
            }
        }
    }

    // 이전 버튼이 눌렸을 때 실행되는 메소드
    @SuppressWarnings({"removal"})
    private void playPreviousTrack() throws FileNotFoundException {
        if(!fileList.isEmpty()) { //리스트에 파일이 있을 때
            index = (index - 1 + fileList.size()) % fileList.size(); //인덱스 감소 - 이전 트랙 음악 선택, 0일 때의 경우 생각
            if(musicThread.isAlive()) { //뮤직 스레드가 살아 있다면
                musicThread.stop();	//뮤직 스레드를 정지
            }
            startMusic(fileList.get(index)); //파일 리스트의 인덱스 번째의 음악, 이전 트랙 재생
        }
        else { //리스트에 파일이 없을 때
            JOptionPane.showMessageDialog(null, "리스트에 파일이 존재하지 않습니다!",
                    "오류", JOptionPane.ERROR_MESSAGE); //오류 메시지 띄우기
        }
    }

    private void showInfoDialog() {
        MII_Dialog dialog = new MII_Dialog("프로그램 정보", 250, 240);//다이얼로그 창 띄우기
        dialog.setVisible(true);
    }

    private boolean confirmExit() {
        int result = JOptionPane.showConfirmDialog(this, "정말 종료하시겠습니까?", "종료",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

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

            if(selectedFiles != null) {
                for (File file : selectedFiles) { //파일 배열의 길이 만큼 i를 증가
                    if (!fileList.contains(file)) { //배열에 저장된 파일이 중복되지 않을 때
                        if (file.getName().toLowerCase().endsWith("mp3")) {
                            //확장자가 mp3일 때

                            fileList.add(file); // 파일 리스트에 음악 파일 추가
                            model.addElement(file.getName()); // 재생 목록 UI에 음악 파일 추가
                            if(!isPlaying) {
                                subTitleLabel.setText("음악 파일을 재생하려면 재생 버튼을 누르세요.");
                            }
                        } else { //확장자가 mp3가 아닐 때
                            JOptionPane.showMessageDialog(null, "파일 형식이 올바르지 않습니다.\n"
                                            + "이 플레이어는 확장자가 mp3인 파일만 지원합니다.",
                                    "경고", JOptionPane.WARNING_MESSAGE);
                        }
                    } else { //배열에 저장된 파일이 중복될 때
                        JOptionPane.showMessageDialog(null, "이미 존재하는 파일입니다.",
                                "알림", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
    }

    public void startMusic(File file) throws FileNotFoundException {
        try {
            currentFile = file; //전역 변수에 매개 변수의 정보를 받는다.(일시 정지 후 다시 재생할 때 필요)
            fileInputStream = new FileInputStream(currentFile); //파일 입력 스트림으로 받아오기
            player = new Player(fileInputStream);

            System.out.println(currentFile.length());

            titleLabel.setText("현재 재생 중인 파일은 " + currentFile.getName() + " 입니다."); //재생 중의 음악의 제목을 표시
            subTitleLabel.setText("음악 재생 중");

            musicThread = new Thread(this); //뮤직 스레드 객체 생성(mp3)
            musicThread.start(); //뮤직 스레드 시작
            isPlaying = true; //파워를 켜짐 상태로 전환
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        } catch (JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }

    private void playNextTrack() throws FileNotFoundException {
        index++;
        if(index >= fileList.size()) { //인덱스가 파일 리스트의 크기와 같아질 떄
            index = 0; //인덱스를 0으로 정한다.
        }
        startMusic(fileList.get(index)); //파일 리스트의 인덱스 번째의 음악을 재생
    }

    private void playNextTrack(boolean doubleClick) throws FileNotFoundException { //다음 트랙 재생 메소드 - 자동으로 다음곡 재생 또는 더블클릭 판정 시

        if(mode == 0) { //반복하지 않는다.
            if(!doubleClick) { //음악 리스트에 더블클릭되지 않았을 때
                index++; //인덱스 증가
            }

            if(index < fileList.size()) { //인덱스가 파일 리스트의 크기보다 작을 때까지
                isPlaying = true;
                startMusic(fileList.get(index)); //파일 리스트의 인덱스 번째의 음악을 재생
            }
            else { //인덱스가 파일 리스트의 크기와 같아질 때 부터
                index = 0; //인덱스를 0으로 정한다.
                if(player.isComplete()) {
                    titleLabel.setText("Seong Eun Song"); //제목을 "Seong Eun Song"으로 변경
                    subTitleLabel.setText("리스트에 있는 모든 음악 재생 끝");
                    if(playPauseButton.getIcon() == pauseIcon) { //재생_일시정지 버튼의 아이콘이 일시정지 아이콘이었다면
                        playPauseButton.setIcon(playIcon); //아이콘을 재생 아이콘으로 변경
                    }
                    isPlaying = false; //파워 꺼짐
                }
            }
        } else if (mode == 1) { //한번 반복한다.
            isPlaying = true;
            startMusic(fileList.get(index));
        } else if (mode == 2) { //전체 반복한다.
            if(!doubleClick) { //음악 리스트에 더블클릭되지 않았을 때
                index++; //인덱스 증가
            }
            isPlaying = true;
            if(index >= fileList.size()) {
                index = 0;
            }
            startMusic(fileList.get(index));
        }

    }

    private void deleteTrack(int[] selectedIndices) {
        DefaultListModel<String> model = (DefaultListModel<String>) playList.getModel();

        // 역순으로 삭제
        for(int j = selectedIndices.length - 1; j >= 0; j--) {
            fileList.remove(selectedIndices[j]); //i 배열의 인덱스 j의 파일을 지운다. 없으면 파일은 남아있는데 이름만 지워지고 인덱스가 재설정이 안된다.
            model.remove(selectedIndices[j]); //재생 목록에서 파일 이름을 지운다. 없으면 이름이 안 지워진다.
        }

        // 인덱스 보정
        if(fileList.size() <= index) {
            index = fileList.size() - 1;
        }

        //갱신
        playList.repaint();
    }

    @Override
    @SuppressWarnings("removal")
    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 1) {
            index = playList.locationToIndex(e.getPoint());
        } else if (playList == e.getComponent() && e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            index = playList.locationToIndex(e.getPoint());
            if(index >= 0) {
                if(musicThread != null && musicThread.isAlive()) {
                    musicThread.stop();
                }
                try {
                    isPlaying = true;
                    doubleClicked = true;
                    playPauseButton.setIcon(pauseIcon);
                    playNextTrack(doubleClicked);
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                doubleClicked = false;
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
    public void run() {
        try {
            player = new Player(fileInputStream); //플레이어 객체 생성, 파일 입력 스트림으로 받아온 mp3 파일이다.

            boolean e = musicThread.isAlive(); //뮤직 스레드가 살아있는지 본다.
            System.out.println(e);

            while (!player.isComplete()) {
                player.play();
            }
            playNextTrack(doubleClicked);
        } catch (JavaLayerException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

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
