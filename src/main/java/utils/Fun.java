package utils;

import com.google.cloud.translate.v3.*;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class Fun {

    public static EmbedBuilder clownMessage(String content) {

        String newContent = randomCaps(content);
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(new Color(249,253,0))
                .setDescription(newContent)
                .setThumbnail("https://res.cloudinary.com/nashex/image/upload/v1613698392/assets/759584001131544597_im3kgg.png");

        return embedBuilder;
    }

    public static String randomCaps(String content) {

        Random random = new Random();
        String newContent = "";
        char[] stringChars = content.toCharArray();
        for (char character : stringChars) {
            if (random.nextBoolean())
                newContent += Character.toUpperCase(character);
            else newContent += Character.toLowerCase(character);;
        }
        return newContent;
    }

    public static String translateText(String messageContent) throws IOException {
        // TODO(developer): Replace these variables before running the sample.
        String projectId = "test-project-293223";
        // Supported Languages: https://cloud.google.com/translate/docs/languages
        String targetLanguage = "zh-TW";
        String text = messageContent;
        return translateText(projectId, "en", translateText(projectId, "zh-TW", translateText(projectId, "ja", translateText(projectId, "ar", text))));
    }

    // Translating Text
    public static String translateText(String projectId, String targetLanguage, String text)
            throws IOException {

        String string = "";
        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            // Supported Locations: `global`, [glossary location], or [model location]
            // Glossaries must be hosted in `us-central1`
            // Custom Models must use the same location as your model. (us-central1)
            LocationName parent = LocationName.of(projectId, "global");

            // Supported Mime Types: https://cloud.google.com/translate/docs/supported-formats
            TranslateTextRequest request =
                    TranslateTextRequest.newBuilder()
                            .setParent(parent.toString())
                            .setMimeType("text/plain")
                            .setTargetLanguageCode(targetLanguage)
                            .addContents(text)
                            .build();

            TranslateTextResponse response = client.translateText(request);

            // Display the translation for each input text provided
            for (Translation translation : response.getTranslationsList()) {
                string += translation.getTranslatedText();
            }
        }
        return string;
    }

}
