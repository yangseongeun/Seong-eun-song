package mp3.ui;

import mp3.MII_Dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * [기능 요약]
 * - 메뉴바, 버튼, 재생목록 UI를 담당
 * - 컨트롤러가 접근할 최소 API만 제공
 *
 * [수정 포인트]
 * setSelectedIndex(int index) 추가
 * -> 현재 재생 중인 곡을 리스트에서도 하이라이트
 *
 * - 역할: "화면 구성" 전용
 * [추가 기능]
 * - 진행 바(JSlider)
 * - 현재 시간 / 전체 시간 라벨
 * - 볼륨 슬라이더
 */

public class MP3PlayerFrame extends JFrame {

    private JMenuItem openMenuItem, exitMenuItem, infoMenuItem;
    private JLabel titleLabel, subTitleLabel, playListLabel;
    private JButton prevButton, playPauseButton, stopButton, nextButton, repeatButton; //5개의 버튼
    private JList<String> playList;

    // [추가]
    private JSlider progressSlider;
    private JLabel currentTimeLabel;
    private JLabel totalTimeLabel;

    // [추가]
    private JSlider volumeSlider;
    private JLabel volumeLabel;

    private ImageIcon icon(String name) {
        var url = getClass().getResource("/images/" + name);
        if (url == null) {
            throw new RuntimeException("이미지 파일을 찾을 수 없습니다: /images/" + name);
        }
        return new ImageIcon(url);
    }

    //5개의 버튼
    private final ImageIcon prevIcon = icon("prev.png");
    private final ImageIcon playIcon = icon("play.png");
    private final ImageIcon pauseIcon = icon("pause.png");
    private final ImageIcon stopIcon = icon("stop.png");
    private final ImageIcon nextIcon = icon("next.png");

    public MP3PlayerFrame(String subject, int width, int height) {
        setTitle(subject);
        setSize(width, height);
        setLocation(800, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // [기능] UI 컴포넌트 생성
        initializeComponents();

        // [기능] 화면 레이아웃 배치
        setupLayout();
    }

    // UI 컴포넌트 초기화
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

        exitMenuItem = new JMenuItem("종료");
        exitMenuItem.setFont(Gullim);
        exitMenuItem.setToolTipText("프로그램을 종료합니다.");

        JMenu infoMenu = new JMenu("정보");
        infoMenu.setFont(Gullim);

        infoMenuItem = new JMenuItem("프로그램 정보");
        infoMenuItem.setFont(Gullim);
        infoMenuItem.setToolTipText("프로그램의 정보입니다.");

        // [기능] ActionCommand로 컨트롤러가 어떤 동작인지 구분
        openMenuItem.setActionCommand("OPEN");
        exitMenuItem.setActionCommand("EXIT");
        infoMenuItem.setActionCommand("INFO");

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

        // 버튼
        prevButton.setActionCommand("PREV");
        playPauseButton.setActionCommand("PLAY_PAUSE");
        stopButton.setActionCommand("STOP");
        nextButton.setActionCommand("NEXT");
        repeatButton.setActionCommand("REPEAT");

        //재생 목록
        playList = new JList<String>(new DefaultListModel<String>());
        playList.setFont(gungseo);

        // [추가] 진행 바
        progressSlider = new JSlider(0, 1000, 0);
        progressSlider.setToolTipText("재생 위치 이동");
        progressSlider.setValue(0);

        currentTimeLabel = new JLabel("00:00");
        totalTimeLabel = new JLabel("00:00");

        // [추가] 볼륨
        volumeLabel = new JLabel("볼륨");
        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setToolTipText("볼륨 조절");
    }

    //버튼을 만드는 함수
    private JButton createButton(ImageIcon icon, String tooltip, Font font) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFont(font);
        return button;
    }

    // 레이아웃
    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(6, 1));
        JPanel titlePanel = new JPanel();
        JPanel subTitlePanel = new JPanel();
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5));
        JPanel progressPanel = new JPanel(new BorderLayout());
        JPanel volumePanel = new JPanel(new BorderLayout());
        JPanel playListPanel = new JPanel();

        buttonPanel.add(prevButton);
        buttonPanel.add(playPauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(repeatButton);

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.add(currentTimeLabel, BorderLayout.WEST);
        timePanel.add(totalTimeLabel, BorderLayout.EAST);

        progressPanel.add(progressSlider, BorderLayout.CENTER);
        progressPanel.add(timePanel, BorderLayout.SOUTH);

        volumePanel.add(volumeLabel, BorderLayout.WEST);
        volumePanel.add(volumeSlider, BorderLayout.CENTER);

        titlePanel.setBackground(new Color(186, 255, 26));
        subTitlePanel.setBackground(Color.WHITE);
        playListPanel.setBackground(new Color(255, 149, 102));
        playList.setBackground(new Color(144, 245, 255));

        titlePanel.add(titleLabel);
        subTitlePanel.add(subTitleLabel);
        playListPanel.add(playListLabel);

        topPanel.add(titlePanel);
        topPanel.add(subTitlePanel);
        topPanel.add(buttonPanel);
        topPanel.add(progressPanel);
        topPanel.add(volumePanel);
        topPanel.add(playListPanel);

        JScrollPane scrollPane = new JScrollPane(playList);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    // [기능] 컨트롤러와 이벤트 연결(여기에 리스너 등록만)
    public void bind(ActionListener action, MouseListener mouse, KeyListener key) {
        openMenuItem.addActionListener(action);
        exitMenuItem.addActionListener(action);
        infoMenuItem.addActionListener(action);

        prevButton.addActionListener(action);
        playPauseButton.addActionListener(action);
        stopButton.addActionListener(action);
        nextButton.addActionListener(action);
        repeatButton.addActionListener(action);

        playList.addMouseListener(mouse);
        playList.addKeyListener(key);
    }

    // [기능] Controller가 사용하는 최소한의 UI 접근 메서드
    public DefaultListModel<String> listModel() {
        return (DefaultListModel<String>) playList.getModel();
    }
    public int listIndexAt(Point p) {
        return playList.locationToIndex(p);
    }
    public int[] selectedIndices() {
        return playList.getSelectedIndices();
    }
    public void repaintPlaylist() {
        playList.repaint();
    }
    public void setTitleText(String t) {
        titleLabel.setText(t);
    }
    public void setSubTitleText(String t) {
        subTitleLabel.setText(t);
    }
    public void setPlayIcon() {
        playPauseButton.setIcon(playIcon);
    }
    public void setPauseIcon() {
        playPauseButton.setIcon(pauseIcon);
    }
    public void setRepeatText(String t) {
        repeatButton.setText(t);
    }

    // [기능 추가] 진행 바 값 세팅
    public void setProgressValue(int value) { progressSlider.setValue(value); }
    public int getProgressValue() { return progressSlider.getValue(); }
    public JSlider progressSlider() { return progressSlider; }

    // [기능 추가] 시간 텍스트
    public void setCurrentTimeText(String text) { currentTimeLabel.setText(text); }
    public void setTotalTimeText(String text) { totalTimeLabel.setText(text); }

    // [기능 추가] 볼륨
    public JSlider volumeSlider() { return volumeSlider; }
    public int getVolumeValue() { return volumeSlider.getValue(); }
    public void setVolumeValue(int value) { volumeSlider.setValue(value); }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= listModel().size()) return;

        playList.setSelectedIndex(index);
        playList.ensureIndexIsVisible(index);
    }

    public boolean confirmExit() {
        int result = JOptionPane.showConfirmDialog(this, "정말 종료하시겠습니까?", "종료",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    public void showNoFileError() {
        JOptionPane.showMessageDialog(this, "리스트에 파일이 존재하지 않습니다!",
                "오류", JOptionPane.ERROR_MESSAGE);
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "오류", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfoDialog() {
        MII_Dialog dialog = new MII_Dialog("프로그램 정보", 250, 240); //다이얼로그 창 띄우기
        dialog.setVisible(true);
    }
}
