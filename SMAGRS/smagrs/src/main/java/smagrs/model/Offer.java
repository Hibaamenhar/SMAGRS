package smagrs.model;

public class Offer implements Comparable<Offer> {
    private String roomLocalName;
    private double price;
    private double score;

    public Offer() {}
    public Offer(String room, double price, double score) {
        this.roomLocalName = room;
        this.price = price;
        this.score = score;
    }

    // Getters
    public String getRoomLocalName() { return roomLocalName; }
    public double getPrice() { return price; }
    public double getScore() { return score; }

    @Override
    public int compareTo(Offer o) {
        int cmp = Double.compare(o.score, this.score);
        return (cmp != 0) ? cmp : Double.compare(this.price, o.price);
    }

    @Override
    public String toString() {
        return "%s | score=%.2f | coût=%.2f".formatted(roomLocalName, score, price);
    }
}
