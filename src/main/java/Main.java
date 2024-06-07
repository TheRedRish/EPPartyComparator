import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        mainMenu();
    }

    public static void mainMenu() {
        System.out.print("Enter GetMatch url: ");

        Scanner scan = new Scanner(System.in);
        String getMatchUrl = scan.nextLine();
        String answers = getAnswers(getMatchUrl);
        if (answers == null) {
            System.out.println("Something went wrong. Please try again.");
            mainMenu();
            return;
        }
        showAverage(answers);
    }

    public static String getAnswers(String getMatchUrl) {
        Pattern pattern = Pattern.compile("(?<=answers=)(.*?)(?=&|$)");
        Matcher matcher = pattern.matcher(getMatchUrl);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public static List<Party> getParties() {
        String urlString = "https://www.dr.dk/nyheder/politik/ep-valg/_next/data/WKL1BAvOtjaAxYB1F5Y5X/din-stemmeseddel.json";
        try {
            // Create URL object
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Get the input stream and read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            // Close the connection
            connection.disconnect();

            // Parse the JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(content.toString());

            // Navigate through the JSON hierarchy
            JsonNode smallConstituencyCandidatesNode = rootNode
                    .path("pageProps")
                    .path("smallConstituencyCandidatesByPartyCode");

            // List to store all candidateIds
            List<Party> parties = new ArrayList<>();

            // Iterate over the array "smallConstituencyCandidatesByPartyCode"
            for (JsonNode partyNode : smallConstituencyCandidatesNode) {
                // Iterate over each party
                Iterator<String> fieldNames = partyNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();

                    String partyName = partyNode.get(fieldName).get("partyName").asText();
                    JsonNode candidatesNode = partyNode.get(fieldName).get("candidates");
                    Party party = new Party(partyName);

                    // Iterate over each candidate
                    for (JsonNode candidateNode : candidatesNode) {
                        int candidateId = candidateNode.get("candidateId").asInt();
                        party.candidateIds.add(candidateId);
                    }
                    parties.add(party);
                }
            }
            return parties;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void showAverage(String answers) {
        List<Party> parties = getParties();
        if (parties == null) {
            return;
        }
        for (Party party : parties) {
            int matchTotal = 0;

            Map<String, Integer> topicMatchTotalMap = new HashMap<>();
            for (int candidateId : party.candidateIds) {
                String urlString = "https://www.dr.dk/nyheder/politik/ep-valg/api/candidate/GetCandidateMatch?answers=" + answers + "&candidateId=" + candidateId;
                try {
                    // Create URL object
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    // Get the input stream and read the response
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    // Close the connection
                    connection.disconnect();

                    // Parse the JSON response
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(content.toString());

                    // Iterate over the array "TopMatches"
                    JsonNode topMatchesNode = rootNode.path("TopMatches");
                    for (JsonNode matchNode : topMatchesNode) {
                        // Extract Matchpercent value
                        int matchPercent = matchNode.path("Matchpercent").asInt();
                        matchTotal += matchPercent;

                        JsonNode topicMatchArray = matchNode.path("TopicMatch");
                        for (JsonNode topicNode : topicMatchArray) {
                            String title = topicNode.path("Title").asText();
                            int topicMatchPercent = topicNode.path("MatchPercent").asInt();
                            // Update topic match total and count for the party
                            topicMatchTotalMap.put(title, topicMatchTotalMap.getOrDefault(title, 0) + topicMatchPercent);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            party.averagePartyPercent = (double) matchTotal / party.candidateIds.size();
            party.topicMatchTotalMap = topicMatchTotalMap;
        }
        showPartyAverage(parties);
    }

    public static void showPartyAverage(List<Party> parties){
        if (parties == null) {
            return;
        }
        Collections.sort(parties);

        for (Party party : parties) {
            System.out.println();
            System.out.print("\u001B[31m" + party.partyName + "\u001B[0m");

            System.out.printf(": %.2f", party.averagePartyPercent);
            System.out.println("%");
            System.out.println();

            // Calculate and print average topic match percentage for the party
            for (Map.Entry<String, Integer> entry : party.topicMatchTotalMap.entrySet()) {
                String topic = entry.getKey();
                int totalMatchPercent = entry.getValue();
                double averageMatchPercent = (double) totalMatchPercent / party.candidateIds.size();
                System.out.println("\u001B[32m" + topic + "\u001B[0m");
                drawPercentageBar(averageMatchPercent);
            }
            System.out.println();
        }
    }

    private static void drawPercentageBar(double percent) {
        int barWidth = 100; // Width of the bar
        int numChars = (int) (percent / 100 * barWidth); // Number of characters representing the percentage
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barWidth; i++) {
            if (i < numChars) {
                bar.append("#");
            } else {
                bar.append(" ");
            }
        }
        bar.append("] ").append((int) (percent)).append("%");
        System.out.println(bar.toString());
    }
}
