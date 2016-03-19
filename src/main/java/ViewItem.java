import java.util.StringTokenizer;

public class ViewItem {
    private long startTime;
    private String userID;
    private String link;
    private long allTime;

    public ViewItem(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        startTime = Integer.parseInt(tokenizer.nextToken())*1000L;
        userID = tokenizer.nextToken();
        link = tokenizer.nextToken();
        allTime = Integer.parseInt(tokenizer.nextToken())*1000L;
    }

    public ViewItem(long startTime, String userID, String link, long allTime) {
        this.startTime = startTime;
        this.userID = userID;
        this.link = link;
        this.allTime = allTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public long getAllTime() {
        return allTime;
    }

    public void setAllTime(long allTime) {
        this.allTime = allTime;
    }

    @Override
    public String toString() {
        return "ViewItem{" +
                "startTime=" + startTime +
                ", userID='" + userID + '\'' +
                ", link='" + link + '\'' +
                ", allTime=" + allTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewItem viewItem = (ViewItem) o;

        if (startTime != viewItem.startTime) return false;
        if (allTime != viewItem.allTime) return false;
        if (userID != null ? !userID.equals(viewItem.userID) : viewItem.userID != null) return false;
        return link != null ? link.equals(viewItem.link) : viewItem.link == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (userID != null ? userID.hashCode() : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (int) (allTime ^ (allTime >>> 32));
        return result;
    }
}
