package com.thk.aktienkursrechner;

public class Aktie {

    private String datum;
    private Double open;
    private Double close;
    private Double rendite;

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Double getRendite() {
        return rendite;
    }

    public void setRendite(Double rendite) {
        this.rendite = rendite;
    }
}
