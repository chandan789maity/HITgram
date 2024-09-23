package com.example.pdfextractor;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

public class NGramTextPredictorColorfulTheme {
    private static JTextArea chatArea;
    private static JTextField inputSentenceField, inputNumWordsField;
    private static JSlider nSlider;
    private static JButton submitButton, uploadButton;
    private static JFileChooser fileChooser;
    private static Map<String, Map<String, Integer>> nGramModel = new HashMap<>();
    private static Map<String, Integer> tokenFrequencyMap = new HashMap<>();
    private static int n = 3; // Default N-Gram size (trigram)
    private static final int VOCAB_SIZE = 10000; // Estimate of vocabulary size for smoothing

    public static void main(String[] args) {
        // Apply colorful theme look and feel
        applyColorfulTheme();

        // GUI Setup
        JFrame frame = new JFrame("N-Gram Chatbot (Colorful Theme)");
        frame.setSize(600, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Chat area (display conversation and tokenization)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(40, 44, 52));
        chatArea.setForeground(Color.WHITE);
        chatArea.setCaretColor(Color.WHITE);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        panel.add(chatScrollPane, BorderLayout.CENTER);

        // Input area (for sentence, N-slider, and number of words to predict)
        JPanel inputPanel = new JPanel(new GridLayout(3, 1));
        inputPanel.setBackground(new Color(255, 255, 255));

        JPanel sentencePanel = new JPanel(new BorderLayout());
        sentencePanel.setBackground(new Color(245, 245, 245));
        inputSentenceField = new JTextField();
        inputSentenceField.setBackground(new Color(85, 239, 196));
        inputSentenceField.setForeground(Color.BLACK);
        inputSentenceField.setCaretColor(Color.BLACK);
        sentencePanel.add(new JLabel("Enter sentence:", JLabel.LEFT), BorderLayout.WEST);
        sentencePanel.add(inputSentenceField, BorderLayout.CENTER);
        inputPanel.add(sentencePanel);

        // Slider panel for selecting N value
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBackground(new Color(245, 245, 245));
        nSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 3);
        nSlider.setMajorTickSpacing(1);
        nSlider.setPaintTicks(true);
        nSlider.setPaintLabels(true);
        nSlider.setBackground(new Color(245, 245, 245));
        nSlider.setForeground(Color.DARK_GRAY);
        sliderPanel.add(new JLabel("Select N for N-Grams:", JLabel.LEFT), BorderLayout.WEST);
        sliderPanel.add(nSlider, BorderLayout.CENTER);
        inputPanel.add(sliderPanel);

        // Panel for number of words to predict
        JPanel numWordsPanel = new JPanel(new BorderLayout());
        numWordsPanel.setBackground(new Color(245, 245, 245));
        inputNumWordsField = new JTextField("3"); // Default value for 3 predicted words
        inputNumWordsField.setBackground(new Color(255, 234, 167));
        inputNumWordsField.setForeground(Color.BLACK);
        inputNumWordsField.setCaretColor(Color.BLACK);
        numWordsPanel.add(new JLabel("Number of words to predict:", JLabel.LEFT), BorderLayout.WEST);
        numWordsPanel.add(inputNumWordsField, BorderLayout.CENTER);
        inputPanel.add(numWordsPanel);

        panel.add(inputPanel, BorderLayout.SOUTH);

        // Buttons for actions
        submitButton = new JButton("Predict Next Words");
        uploadButton = new JButton("Upload and Build Model");

        // Style buttons with bright accent colors
        submitButton.setBackground(new Color(255, 121, 121));
        submitButton.setForeground(Color.WHITE);
        uploadButton.setBackground(new Color(129, 236, 236));
        uploadButton.setForeground(Color.BLACK);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.add(uploadButton);
        buttonPanel.add(submitButton);
        panel.add(buttonPanel, BorderLayout.NORTH);

        // File chooser for uploading corpus
        fileChooser = new JFileChooser();

        // Action listener for the upload button for tokenization and N-Gram model building
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    n = nSlider.getValue();  // Get the value from the slider
                    try {
                        if (file.getName().endsWith(".pdf")) {
                            tokenizeAndCalculateProbabilityFromPDF(file);
                        } else {
                            buildNGramModel(file, n);
                            tokenizeAndCalculateProbability(file);
                        }
                    } catch (IOException ioException) {
                        chatArea.append("Bot: Error reading the uploaded file. Please check the path.\n");
                    }
                } else {
                    chatArea.append("Bot: No file selected.\n");
                }
            }
        });

        // Action listener for the submit button for prediction
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputSentence = inputSentenceField.getText().trim();
                String numWordsStr = inputNumWordsField.getText().trim();

                if (!inputSentence.isEmpty() && !numWordsStr.isEmpty()) {
                    try {
                        int nTokens = Integer.parseInt(numWordsStr); // Get number of words to predict from input
                        chatArea.append("User: " + inputSentence + "\n");
                        String botResponse = predictNextWords(inputSentence, nTokens);
                        chatArea.append("Bot: " + botResponse + "\n");
                    } catch (NumberFormatException ex) {
                        chatArea.append("Bot: Please enter a valid number of words to predict.\n");
                    }
                } else {
                    chatArea.append("Bot: Please provide both a sentence and number of words to predict.\n");
                }
            }
        });

        frame.add(panel);
        frame.setVisible(true);
    }

    // Apply a colorful theme to the UI
    private static void applyColorfulTheme() {
        UIManager.put("Panel.background", new ColorUIResource(245, 245, 245));
        UIManager.put("Label.foreground", Color.DARK_GRAY);
        UIManager.put("TextField.background", new ColorUIResource(255, 234, 167));
        UIManager.put("TextField.foreground", new ColorUIResource(Color.BLACK));
        UIManager.put("Button.background", new ColorUIResource(129, 236, 236));
        UIManager.put("Button.foreground", new ColorUIResource(Color.BLACK));
        UIManager.put("Slider.background", new ColorUIResource(245, 245, 245));
        UIManager.put("Slider.foreground", new ColorUIResource(Color.DARK_GRAY));
    }

    // Build N-Gram model from a user-uploaded corpus with Laplace Smoothing
    private static void buildNGramModel(File file, int n) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder corpusBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                corpusBuilder.append(line).append(" ");
            }
            reader.close();
            String corpus = corpusBuilder.toString().trim();
            buildNGramModel(corpus, n);
            chatArea.append("Bot: N-Gram model successfully built from the uploaded corpus file.\n");
        } catch (IOException e) {
            chatArea.append("Bot: Error reading the uploaded file. Please check the path.\n");
        }
    }

    // Build N-Gram model from a corpus
    private static void buildNGramModel(String corpus, int n) {
        String[] words = corpus.split("\\s+");
        nGramModel.clear(); // Clear the existing model before building a new one
        tokenFrequencyMap.clear(); // Clear token frequency map

        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder keyBuilder = new StringBuilder();
            for (int j = i; j < i + n - 1; j++) {
                keyBuilder.append(words[j]).append(" ");
            }
            String key = keyBuilder.toString().trim();
            String nextWord = words[i + n - 1];

            nGramModel.putIfAbsent(key, new HashMap<>());
            nGramModel.get(key).put(nextWord, nGramModel.get(key).getOrDefault(nextWord, 0) + 1);

            // Update token frequency map for Laplace Smoothing
            tokenFrequencyMap.put(key, tokenFrequencyMap.getOrDefault(key, 0) + 1);
        }
    }

    private static String predictNextWords(String sentence, int numTokens) {
        if (numTokens <= 0) {
            return "Please enter a positive number of tokens.";
        }

        String[] words = sentence.split("\\s+");
        StringBuilder prediction = new StringBuilder();

        for (int i = 0; i < numTokens; i++) {
            // Build the key based on the last (N-1) words
            StringBuilder keyBuilder = new StringBuilder();
            for (int j = Math.max(0, words.length - n + 1); j < words.length; j++) {
                keyBuilder.append(words[j]).append(" ");
            }
            String key = keyBuilder.toString().trim();

            Map<String, Integer> possibleWords = nGramModel.get(key);
            if (possibleWords != null && !possibleWords.isEmpty()) {
                // Laplace Smoothing: Calculate probabilities
                String nextWord = null;
                double maxProbability = -1;
                for (Map.Entry<String, Integer> entry : possibleWords.entrySet()) {
                    double smoothedProbability = (entry.getValue() + 1) / (double) (tokenFrequencyMap.getOrDefault(key, 0) + VOCAB_SIZE);
                    if (smoothedProbability > maxProbability) {
                        maxProbability = smoothedProbability;
                        nextWord = entry.getKey();
                    }
                }

                prediction.append(nextWord).append(" ");

                // Update the sentence with the predicted word
                sentence += " " + nextWord;
                words = sentence.split("\\s+");
            } else {
                chatArea.append("Bot: No matching n-gram key found for: \"" + key + "\".\n");
                return "Sorry, I couldn't predict the next words based on the given input.";
            }
        }

        return prediction.toString().trim();
    }

    // Tokenize and calculate probability from PDF file
    private static void tokenizeAndCalculateProbabilityFromPDF(File file) throws IOException {
        PDDocument document = PDDocument.load(file);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String corpus = pdfStripper.getText(document);
        document.close();

        // Tokenize and calculate frequency
        String[] tokens = corpus.split("\\s+");
        tokenFrequencyMap.clear();
        for (String token : tokens) {
            tokenFrequencyMap.put(token, tokenFrequencyMap.getOrDefault(token, 0) + 1);
        }

        // Calculate probability
        chatArea.append("Token\t\tFrequency\t\t\tProbability\n");
        for (Map.Entry<String, Integer> entry : tokenFrequencyMap.entrySet()) {
            double probability = entry.getValue() / (double) tokens.length;
            chatArea.append(entry.getKey() + "\t\t" + entry.getValue() + "\t\t" + probability + "\n");
        }
    }

    // Tokenize the file and calculate the probability of each token
    private static void tokenizeAndCalculateProbability(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder corpusBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                corpusBuilder.append(line).append(" ");
            }
            reader.close();
            String corpus = corpusBuilder.toString().trim();

            // Tokenize and calculate frequency
            String[] tokens = corpus.split("\\s+");
            tokenFrequencyMap.clear();
            for (String token : tokens) {
                tokenFrequencyMap.put(token, tokenFrequencyMap.getOrDefault(token, 0) + 1);
            }

            // Calculate probability
            chatArea.append("Token\t\tFrequency\t\t\tProbability\n");
            for (Map.Entry<String, Integer> entry : tokenFrequencyMap.entrySet()) {
                double probability = entry.getValue() / (double) tokens.length;
                chatArea.append(entry.getKey() + "\t\t" + entry.getValue() + "\t\t" + probability + "\n");
            }

        } catch (IOException e) {
            chatArea.append("Bot: Error reading the uploaded file. Please check the path.\n");
        }
    }
}
