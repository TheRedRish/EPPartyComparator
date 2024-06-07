import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Party implements Comparable<Party> {
    public String partyName;
    public List<Integer> candidateIds;

    public double averagePartyPercent;
    public Map<String, Integer> topicMatchTotalMap;

    public Party(String partyName) {
        this.partyName = partyName;
        this.candidateIds = new ArrayList<>();

        this.averagePartyPercent = 0;
        this.topicMatchTotalMap = null;
    }

    @Override
    public int compareTo(Party other) {
        return Double.compare(other.averagePartyPercent, this.averagePartyPercent);
    }
}
