package utils;

import javax.imageio.ImageIO;
import javax.swing.*;

import image.ImageProcessingUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PickImageOption {
    private int userInput = -1;

    int sig[] = null;
    IntPair prop=null;
	Signature[] sigarray;

    public PickImageOption(int sig[], 	Signature[] sigarray, IntPair prop) {
    	this.sig=sig;
    	this.prop=prop;
    	this.sigarray = sigarray;
    }
    public void pick() {
        createAndShowWindow();

        while (userInput == -1) {
            try {
                Thread.sleep(100); // Pause briefly to allow user input
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("User input received: " + userInput);
    }

    private String msg(int i) {
    	return ""+sigarray[sig[i]].sqrd+"::"+sigarray[sig[i]].filename;
    }

    private JPanel imagePanel(int i) {
    	JPanel imagePanelWithLabel = new JPanel(new BorderLayout(1,1));        
    	JPanel imageDisplayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage img = ImageProcessingUtils.get(sigarray[sig[i]], prop,Proportion.getPropNumberOfPixels()); 
                if (img != null) {
                    g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        imageDisplayPanel.setPreferredSize(new Dimension(428, 321));
        JLabel msgLabel = new JLabel(msg(i));
        imagePanelWithLabel.add(imageDisplayPanel, BorderLayout.NORTH);
        imagePanelWithLabel.add(msgLabel, BorderLayout.SOUTH);
        
        return imagePanelWithLabel;
    }

    private void createAndShowWindow() {
        JFrame frame = new JFrame("Imagens");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel outerPanel = new JPanel(new GridLayout(0,2,3,3)); 

        JPanel inputPanel = new JPanel();
        JLabel promptLabel = new JLabel("Enter a number (1-4): ");
        JTextField inputField = new JTextField(5);

        inputPanel.add(promptLabel);
        inputPanel.add(inputField);

        for (int i=0;i<4;++i) {
        	outerPanel.add(imagePanel(i));
        }
        
        // Action listener for the input field
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int value = Integer.parseInt(inputField.getText().trim());
                    if (value >= 1 && value <= 4) {
                        userInput = value;
                        frame.dispose(); // Close the window
                    } else {
                        JOptionPane.showMessageDialog(frame, "Please enter a number between 1 and 4.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid input. Please enter a number between 1 and 4.");
                }
            }
        });
        frame.add(outerPanel, BorderLayout.CENTER);
        //outerPanel.add(inputPanel, BorderLayout.SOUTH);
        frame.add(inputPanel, BorderLayout.SOUTH);
        //frame.setContentPane(outerPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
