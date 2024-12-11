package com.ngram.HITgram;

import javax.swing.*;
import java.util.List;
import javax.swing.plaf.ColorUIResource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

public class HITgram {
    private static JTextArea chatArea;
    private static JTextField inputSentenceField, inputNumWordsField;
    private static JSlider nSlider;
    private static JButton submitButton, uploadButton, perplexityButton;
    private static JFileChooser fileChooser;
    private static Map<String, Map<String, Integer>> nGramModel = new HashMap<>();
    private static Map<String, Integer> tokenFrequencyMap = new HashMap<>();
    private static int n = 2; // Default N-Gram size (bigram)
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
        nSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 2);
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
        perplexityButton = new JButton("Calculate Perplexity");

        // Style buttons with bright accent colors
        submitButton.setBackground(new Color(255, 121, 121));
        submitButton.setForeground(Color.WHITE);
        uploadButton.setBackground(new Color(129, 236, 236));
        uploadButton.setForeground(Color.BLACK);
        perplexityButton.setBackground(new Color(250, 177, 160));
        perplexityButton.setForeground(Color.BLACK);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.add(uploadButton);
        buttonPanel.add(submitButton);
        buttonPanel.add(perplexityButton);
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
                        long startTime = System.currentTimeMillis(); // Start timer
                        if (file.getName().endsWith(".pdf")) {
                            tokenizeAndCalculateProbabilityFromPDF(file);
                        } else {
                            buildNGramModel(file, n);
                        }
                        long endTime = System.currentTimeMillis(); // End timer
                        long duration = endTime - startTime; // Calculate duration
                        chatArea.append("Bot: N-Gram model successfully built from the uploaded corpus file in " + duration + " ms.\n");
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

        // Action listener for calculating perplexity of a test sentence
        perplexityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputSentence = inputSentenceField.getText().trim();
                if (!inputSentence.isEmpty()) {
                    chatArea.append("User: " + inputSentence + "\n");
                    double perplexity = calculatePerplexity(inputSentence);
                    chatArea.append("Bot: Perplexity of the sentence: " + perplexity + "\n");
                } else {
                    chatArea.append("Bot: Please provide a sentence to calculate perplexity.\n");
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

    // Build N-Gram model from the given corpus string
    private static void buildNGramModel(String corpus, int n) {
        nGramModel.clear();
        tokenFrequencyMap.clear();

        String[] tokens = corpus.toLowerCase().replaceAll("[^a-zA-Z ]", "").split("\\s+");
        for (int i = 0; i <= tokens.length - n; i++) {
            StringBuilder nGramBuilder = new StringBuilder();
            for (int j = 0; j < n - 1; j++) {
                nGramBuilder.append(tokens[i + j]).append(" ");
            }
            String nGramPrefix = nGramBuilder.toString().trim();
            String nextToken = tokens[i + n - 1];

            tokenFrequencyMap.put(nextToken, tokenFrequencyMap.getOrDefault(nextToken, 0) + 1);

            nGramModel.putIfAbsent(nGramPrefix, new HashMap<>());
            Map<String, Integer> suffixMap = nGramModel.get(nGramPrefix);
            suffixMap.put(nextToken, suffixMap.getOrDefault(nextToken, 0) + 1);
        }
    }

    // Predict the next word based on N-Gram model with smoothing
    private static String predictNextWords(String inputSentence, int numWordsToPredict) {
        String[] inputTokens = inputSentence.toLowerCase().replaceAll("[^a-zA-Z ]", "").split("\\s+");
        StringBuilder result = new StringBuilder();
        List<String> context = new ArrayList<>(Arrays.asList(inputTokens));

        for (int i = 0; i < numWordsToPredict; i++) {
            StringBuilder nGramBuilder = new StringBuilder();
            for (int j = Math.max(0, context.size() - (n - 1)); j < context.size(); j++) {
                nGramBuilder.append(context.get(j)).append(" ");
            }
            String nGramPrefix = nGramBuilder.toString().trim();
            String predictedWord = predictNextWordForNGram(nGramPrefix);
            context.add(predictedWord);
            result.append(predictedWord).append(" ");
        }
        return result.toString().trim();
    }

    // Predict the next word using N-Gram model and Laplace smoothing
    private static String predictNextWordForNGram(String nGramPrefix) {
        Map<String, Integer> suffixMap = nGramModel.getOrDefault(nGramPrefix, new HashMap<>());
        int totalSuffixCount = suffixMap.values().stream().mapToInt(Integer::intValue).sum();
        double maxProbability = -1.0;
        String predictedWord = null;

        for (Map.Entry<String, Integer> entry : suffixMap.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();
            double probability = (count + 1.0) / (totalSuffixCount + VOCAB_SIZE); // Laplace smoothing
            if (probability > maxProbability) {
                maxProbability = probability;
                predictedWord = word;
            }
        }
        if (predictedWord == null) {
            predictedWord = "unknown"; // Handle case where no suitable word is found
        }
        return predictedWord;
    }

    // Calculate probability of a given sentence using the N-Gram model
    private static double calculateProbability(String sentence) {
        String[] tokens = sentence.toLowerCase().replaceAll("[^a-zA-Z ]", "").split("\\s+");
        double probability = 1.0;

        for (int i = 0; i <= tokens.length - n; i++) {
            StringBuilder nGramBuilder = new StringBuilder();
            for (int j = 0; j < n - 1; j++) {
                nGramBuilder.append(tokens[i + j]).append(" ");
            }
            String nGramPrefix = nGramBuilder.toString().trim();
            String nextToken = tokens[i + n - 1];
            Map<String, Integer> suffixMap = nGramModel.getOrDefault(nGramPrefix, new HashMap<>());
            int count = suffixMap.getOrDefault(nextToken, 0);
            int totalSuffixCount = suffixMap.values().stream().mapToInt(Integer::intValue).sum();
            probability *= (count + 1.0) / (totalSuffixCount + VOCAB_SIZE); // Laplace smoothing
        }
        return probability;
    }

    // Calculate perplexity of a sentence
    private static double calculatePerplexity(String sentence) {
        double probability = calculateProbability(sentence);
        int numTokens = sentence.split("\\s+").length;
        return Math.pow(1.0 / probability, 1.0 / numTokens);
    }

    // Tokenize PDF file and calculate the N-Gram model from the text content
    private static void tokenizeAndCalculateProbabilityFromPDF(File file) throws IOException {
        PDDocument document = PDDocument.load(file);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String pdfText = pdfStripper.getText(document);
        document.close();
        buildNGramModel(pdfText, n);
    }
}
