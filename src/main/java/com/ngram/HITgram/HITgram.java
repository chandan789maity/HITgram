

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
            StringBuilder nGram = new StringBuilder();
            for (int j = 0; j < n - 1; j++) {
                nGram.append(words[i + j]).append(" ");
            }
            nGram = new StringBuilder(nGram.toString().trim());
            String nextWord = words[i + n - 1];

            nGramModel.putIfAbsent(nGram.toString(), new HashMap<>());
            Map<String, Integer> nextWordMap = nGramModel.get(nGram.toString());
            nextWordMap.put(nextWord, nextWordMap.getOrDefault(nextWord, 0) + 1);

            // Update frequency map
            tokenFrequencyMap.put(nextWord, tokenFrequencyMap.getOrDefault(nextWord, 0) + 1);
        }
    }

    // Predict the next word(s) based on the input text
    private static String predictNextWords(String inputText, int numWordsToPredict) {
        StringBuilder result = new StringBuilder();
        String[] inputTokens = inputText.split("\\s+");

        for (int i = 0; i < numWordsToPredict; i++) {
            StringBuilder nGramKey = new StringBuilder();
            int start = Math.max(0, inputTokens.length - n + 1);

            for (int j = start; j < inputTokens.length; j++) {
                nGramKey.append(inputTokens[j]).append(" ");
            }
            String key = nGramKey.toString().trim();

            if (nGramModel.containsKey(key)) {
                Map<String, Integer> nextWordMap = nGramModel.get(key);
                String predictedWord = nextWordMap.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .get()
                        .getKey();
                result.append(predictedWord).append(" ");
                inputTokens = Arrays.copyOf(inputTokens, inputTokens.length + 1);
                inputTokens[inputTokens.length - 1] = predictedWord;
            } else {
                break;
            }
        }

        return result.toString().trim();
    }

// Tokenize and build N-Gram model from a PDF file
private static void tokenizeAndCalculateProbabilityFromPDF(File pdfFile) throws IOException {
    long startTime = System.currentTimeMillis(); // Start timer for PDF to text
    PDDocument document = PDDocument.load(pdfFile);
    PDFTextStripper pdfStripper = new PDFTextStripper();
    String text = pdfStripper.getText(document);
    document.close();
    long endTime = System.currentTimeMillis(); // End timer for PDF to text
    long conversionDuration = endTime - startTime; // Calculate PDF to text conversion time
    
    // Calculate the byte size of the text after conversion
    int textSizeInBytes = text.getBytes().length;
    
    // Convert size to KB for better readability
    double textSizeInKB = textSizeInBytes / 1024.0;
    
    chatArea.append("Bot: PDF successfully converted to text in " + conversionDuration + " ms.\n");
    chatArea.append("Bot: The size of the converted text file is approximately " + String.format("%.2f", textSizeInKB) + " KB.\n");

    // Start timer for tokenization
    long tokenizationStartTime = System.currentTimeMillis();
    
    // Now, proceed with building the N-gram model
    buildNGramModel(text, n);

    // End timer for tokenization
    long tokenizationEndTime = System.currentTimeMillis();
    long tokenizationDuration = tokenizationEndTime - tokenizationStartTime; // Calculate tokenization time
    
    chatArea.append("Bot: Tokenization completed in " + tokenizationDuration + " ms.\n");
}

}