package mp3.ui;

import mp3.MII_Dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Refactored version:
 * - Removed Runnable + manual Thread control (stop/suspend/resume).
 * - Uses ExecutorService + Future cancellation + stream close (cooperative cancellation).
 * - UI updates are executed on Swing EDT.
 */

public class MP3PlayerFrame extends JFrame {
    // ===== 기존 UI 필드들 =====
    private JMenuItem openMenuItem, exitMenuItem, infoMenuItem;
    private JLabel titleLabel, subTitleLabel, playListLabel;
    private JButton prevButton, playPauseButton, stopButton, nextButton, repeatButton; //5개의 버튼
    private JList<String> playList;

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

        exitMenuItem = new JMenuItem("종료");
        exitMenuItem.setFont(Gullim);
        exitMenuItem.setToolTipText("프로그램을 종료합니다.");

        JMenu infoMenu = new JMenu("정보");
        infoMenu.setFont(Gullim);

        infoMenuItem = new JMenuItem("프로그램 정보");
        infoMenuItem.setFont(Gullim);
        infoMenuItem.setToolTipText("프로그램의 정보입니다.");

        // 메뉴
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
        playList.setFont(dotum);
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

    // 컨트롤러 바인딩(여기에 리스너 등록만)
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

    // ===== Controller가 쓰는 최소 API =====
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
