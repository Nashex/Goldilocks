package parsing;

import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import main.Goldilocks;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ImageOcr {

    public static List<String> retrievePlayersFromOcr(String filePath) throws IOException {
        List<String> playerList = new ArrayList<>();

        String playerString = detectText(filePath);
        playerList.add(playerString.replace("|", "").replace("0", "o"));
        playerString = detectText(filePath).split(":")[1].replace("\n", "");
        for (String player : playerString.split(",")) {
            playerList.add(player.trim().toLowerCase().replace("0", "o")
                    .replace("|", "l")
                    .replace("г", "r")
                    .replace("ą", "q"));
            //System.out.println(player);
        }

        return playerList;
    }

    public static String detectText(String filePath) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        String outputString = "";
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return "Error in OCR reading";
                }

                outputString += res.getTextAnnotations(0).getDescription();
            }
        }
        return outputString;
    }

    public static HashMap<String, Boolean> detectVaultPicture(File file, String name, String discordTag) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        HashMap<String, Boolean> fields = new HashMap<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(file.getPath()));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        BufferedImage annotatedImage = ImageIO.read(file);
        Graphics2D g2d = annotatedImage.createGraphics();
        g2d.setColor(Color.GREEN);

        //Calculate X,Y limits
        //Image Size
        int imgWidth = annotatedImage.getWidth();
        int imgHeight = annotatedImage.getHeight();
        //Name Bounds
        int nameX = (int) Math.round(imgWidth * .075);
        int nameY = (int) Math.round(imgHeight * .05);
        //Chat Name and Tag Bounds
        int CnameX = (int) Math.round(imgWidth * .15);
        int CnameY = imgHeight - (int) Math.round(imgHeight * .15);
        //Chat Bubble
        int BnameX1 = (int) Math.round(imgWidth * .35);
        int BnameX2 = (int) Math.round(imgWidth * .45);
        int BnameY1 = (int) Math.round(imgHeight * .3);
        int BnameY2 = (int) Math.round(imgHeight * .7);
        //Vault
        int vaultX = imgWidth - (int) Math.round(imgWidth * .15);
        int vaultY = imgHeight - (int) Math.round(imgHeight * .25);

        String outputString = "";
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    if (annotation.getDescription().split("\n").length < 3) {
                        BoundingPoly boundingPoly  = annotation.getBoundingPoly();
                        int x = boundingPoly.getVertices(0).getX();
                        int y = boundingPoly.getVertices(0).getY();
                        int width = boundingPoly.getVertices(1).getX() - boundingPoly.getVertices(0).getX();
                        int height = boundingPoly.getVertices(3).getY() - boundingPoly.getVertices(0).getY();

                        if (annotation.getDescription().equalsIgnoreCase("Vault")) {
                            //System.out.println("Vx:" + vaultX + " Vy:" + vaultY);
                            //System.out.println("X:" + x + " Y:" + y + " Width:" + width + " Height:" + height);
                            if (x > vaultX && y > vaultY) {
                                fields.put("Vault", true);
                                g2d.setColor(Goldilocks.BLUE);
                                g2d.drawRect(x, y, width, height);
                            }
                        }

                        if (annotation.getDescription().toLowerCase().contains(name.toLowerCase()) || annotation.getDescription().toLowerCase().contains(discordTag.toLowerCase())) {
                            //System.out.println(annotation.getBoundingPoly());
                            if (x < nameX && y < nameY && annotation.getDescription().replaceAll("[^0-9A-Za-z]", "").equalsIgnoreCase(name)) {
                                fields.put("Name", true);
                                g2d.setColor(Color.cyan);
                            }

                            if (x < CnameX && y > CnameY && annotation.getDescription().replaceAll("[^0-9A-Za-z]", "").equalsIgnoreCase(name)) {
                                fields.put("Name In Chat", true);
                                g2d.setColor(Color.cyan);
                            }

                            if (x < CnameX && y > CnameY && annotation.getDescription().equalsIgnoreCase(discordTag)) {
                                fields.put("Discord Tag In Chat", true);
                                g2d.setColor(Color.GREEN);
                            }

                            if (annotation.getDescription().toLowerCase().contains(discordTag.toLowerCase())) {
                                //System.out.println("Bx1:" + BnameX1 + " Bx2:" + BnameX2 + " By1:" + BnameY1 + " By2:" + BnameY2);
                                //System.out.println("X:" + x + " Y:" + y + " Width:" + width + " Height:" + height);
                            }

                            if (x > BnameX1 && x < BnameX2 && y > BnameY1 && y < BnameY2 && annotation.getDescription().equalsIgnoreCase(discordTag)) {
                                fields.put("Discord Tag Bubble", true);
                                g2d.setColor(Color.GREEN);
                            }

                            g2d.drawRect(x - 2, y - 2, width + 4, height + 4);
                            g2d.setColor(Color.RED);
                        }
                    }
                }
                //if (res.getTextAnnotations(0).getDescription().contains("ArkAngelzz")) System.out.println(res.getTextAnnotations(0).getBoundingPoly());
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return null;
                }

                outputString += res.getTextAnnotations(0).getDescription();
            }
        }

        g2d.dispose();
        ImageIO.write(annotatedImage, "png", file);

        List<String> reqFields = Arrays.asList(new String[]{"Name", "Vault", "Discord Tag Bubble", "Name In Chat", "Discord Tag In Chat"});
        for (String s : reqFields) {
            if (!fields.containsKey(s)) fields.put(s, false);
        }

        return fields;
    }

    public static HashMap<String, Integer> getPlayerExalts(String filePath, String username) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        HashMap<String, String> foundValues = new HashMap<>();
        HashMap<String, Integer> foundStats = new HashMap<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        File file = new File(filePath);

        BufferedImage annotatedImage = ImageIO.read(file);
        //System.out.println(filePath);
        Graphics2D g2d = annotatedImage.createGraphics();
        g2d.setColor(Color.GREEN);

        String outputString = "";
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            List<String> whiteListedWords = Arrays.asList(new String[]{"ATT","DEF","SPD","DEX","VIT","WIS","LIFE","MANA", "Total Progress", "Exaltations", "HP", "MP", "Vault", "Nexus", "Blacksmith"});
            List<String> stats = Arrays.asList(new String[]{"ATT","DEF","SPD","DEX","VIT","WIS","LIFE","MANA"});

            String[] fields = responses.get(0).getTextAnnotations(0).getDescription().split("\n");
            List<String> foundFields = new ArrayList<>();
            //responses.remove(0);

            for (String s : fields) {
                //System.out.println("F: " + s);
                for (String s2 : whiteListedWords) {
                    if (s.contains(s2) || s.equalsIgnoreCase(username)) {
                        String[] args = s.split(" ");
                        if (args.length > 1 && args[1].contains("/")) {
                            foundValues.put(args[0], String.join("", args).replace(args[0], "").replace(" ", ""));
                        }
                        if (s.equalsIgnoreCase(username)) foundStats.put("Username", 1);
                        foundFields.add(args[0]);

                        if (s2.equalsIgnoreCase("Total Progress") || s2.equalsIgnoreCase("Exaltations")) foundStats.merge("ExaltPointers", 1, Integer::sum);
                        if (s2.equalsIgnoreCase("Nexus") || s2.equalsIgnoreCase("Vault") || s2.equalsIgnoreCase("Blacksmith")) foundStats.merge("VaultPointers", 1, Integer::sum);

                    }
                }
                for (String s2 : stats) {
                    if (s.contains(s2)) {
                        String[] args = s.split(" ");
                        if (args.length > 1 && args[1].contains("/")) {
                            foundStats.put(s2, Integer.parseInt(args[1].split("/")[0]));
                        }
                    }
                }

            }

            for (AnnotateImageResponse res : responses) {
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    if (annotation.getDescription().split("\n").length < 3) {
                        if (foundFields.contains(annotation.getDescription()) || foundValues.containsValue(annotation.getDescription())) {
                            BoundingPoly boundingPoly  = annotation.getBoundingPoly();
                            g2d.drawRect(boundingPoly.getVertices(0).getX(),boundingPoly.getVertices(0).getY(), boundingPoly.getVertices(1).getX() - boundingPoly.getVertices(0).getX(), boundingPoly.getVertices(3).getY() - boundingPoly.getVertices(0).getY());
                        }
                    }
                }
                //if (res.getTextAnnotations(0).getDescription().contains("ArkAngelzz")) System.out.println(res.getTextAnnotations(0).getBoundingPoly());
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return null;
                }

                outputString += res.getTextAnnotations(0).getDescription();
            }
        }

        g2d.dispose();
        ImageIO.write(annotatedImage, "png", file);

        return foundStats;
    }

}
