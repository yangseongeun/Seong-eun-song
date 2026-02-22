package player;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class PlayerTest extends JFrame implements ActionListener, Runnable {
    JButton play, pause, stop, resume;
    Thread t;

    public PlayerTest() {
        play = new JButton("play");
        pause = new JButton("pause");
        stop = new JButton("Stop");
        resume = new JButton("Resume");

        play.addActionListener(this);
        pause.addActionListener(this);
        stop.addActionListener(this);
        resume.addActionListener(this);

        add(play);
        add(pause);
        add(stop);
        add(resume);

        setLayout(new FlowLayout());

        setVisible(true);
    }

    @SuppressWarnings({"removal"})
    @Override
    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();
        if(obj == play) {
            t = new Thread(this);
            t.start();
        } else if (obj == pause) {
            t.suspend();
        } else if (obj == stop) {
            t.stop();
        } else if (obj == resume) {
            t.resume();
        }

    }

    @Override
    public void run() {
        FileInputStream fis;
        try {
            fis = new FileInputStream("C:\\Users\\yang_user\\Downloads\\5노래를 찾는 사람들_사계_192 [music].mp3");
            Player player = new Player(fis);
            player.play();
        } catch (JavaLayerException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
