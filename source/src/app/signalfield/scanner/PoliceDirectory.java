package app.signalfield.scanner;

import java.util.*;

/** Offline reference entries, never a classifier for live Wi-Fi/BLE observations. */
public final class PoliceDirectory {
    public static final String[] FILTERS={"Police / sheriff","Shared LWIN site","Corrections","Other / unverified","All entries"};
    public static final String SYSTEM="https://www.radioreference.com/db/sid/4347";
    public static final String SITE="https://www.radioreference.com/db/site/16177";
    public static final String PARISH="https://www.radioreference.com/db/browse/ctid/1145";
    public static final class Entry {
        public final String name,value,kind,details,source,provenance;
        public final int category;
        Entry(String name,String value,String kind,int category,String details,String source,String provenance){this.name=name;this.value=value;this.kind=kind;this.category=category;this.details=details;this.source=source;this.provenance=provenance;}
        public String displayValue(){return kind.equals("Talkgroup")?"TGID "+value:value+" MHz";}
        public boolean matches(int selected,String query){return (selected==4||selected==category)&&(name+" "+displayValue()+" "+kind+" "+details).toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));}
    }
    private static final List<Entry> ENTRIES;
    static {
        ArrayList<Entry> e=new ArrayList<>();
        e.add(new Entry("Ouachita Sheriff · backup","156.210","Conventional",0,"FMN · 103.5 PL · KNDQ918 · OPSO VHF. Backup to trunked systems.",PARISH,"User screenshot #2; current activity not independently verified."));
        e.add(new Entry("Ouachita Sheriff · interoperability","158.7375","Conventional",0,"FMN · WPNZ949 · OPSO INTEROP. Sheriff VHF interoperability; not dedicated dispatch.",PARISH,"User screenshot #2; current activity not independently verified."));
        e.add(new Entry("Ouachita Sheriff · main dispatch","53000","Talkgroup",0,"LWIN P25 · channel 1 · source mode D. TGID, not an MHz frequency.",SYSTEM,"RadioReference LWIN listing checked 2026-11-04; availability may change."));
        e.add(new Entry("Ouachita Sheriff · secondary dispatch","53001","Talkgroup",0,"LWIN P25 · channel 2 · source mode D. TGID, not an MHz frequency.",SYSTEM,"RadioReference LWIN listing checked 2026-11-04; availability may change."));
        e.add(new Entry("Monroe Police · patrol dispatch","53050","Talkgroup",0,"LWIN P25 · channel 1 · source mode D enc (encryption indicated). No promise of monitorable audio.",SYSTEM,"RadioReference LWIN listing checked 2026-11-04; availability may change."));
        e.add(new Entry("West Monroe Police · dispatch","53055","Talkgroup",0,"LWIN P25 · source mode D. TGID, not an MHz frequency.",SYSTEM,"RadioReference LWIN listing checked 2026-11-04; availability may change."));
        e.add(new Entry("Parish Correctional Center 1","155.9025","Conventional",2,"FMN · 205 DPL · WQKH269 · OPCC 1. Corrections, not police dispatch.",PARISH,"User screenshot #2; current activity not independently verified."));
        e.add(new Entry("Parish Correctional Center 2","155.5575","Conventional",2,"FMN · 251 DPL · WQKH269 · OPCC 2. Corrections, not police dispatch.",PARISH,"User screenshot #2; current activity not independently verified."));
        for(String f:new String[]{"769.08125","770.15625","770.69375","771.04375","772.00625","772.33125","772.88125","773.13125","773.59375","773.98125","774.31875","774.58125"}){
            boolean control=f.equals("774.31875")||f.equals("774.58125");
            e.add(new Entry("LWIN Monroe · "+(control?"control channel":"shared site channel"),f,"Site frequency",1,"P25 Phase I · RFSS 3 / site 002 · NAC 1BC. "+(control?"Listed control channel. ":"")+"Shared infrastructure; frequency alone does not identify police traffic. Use agency talkgroup IDs on a compatible receiver.",SITE,"User screenshot #7 and RadioReference site listing checked 2026-11-04. Site page updated 2021-04-22."));
        }
        String[] commercial={"451.7875","452.3375","453.0125"};String[] lcn={"04","12","16"};
        for(int i=0;i<commercial.length;i++)e.add(new Entry("Shreveport Communications · Monroe",commercial[i],"Site frequency",3,"400 MHz LTR Standard · analog · LCN "+lcn[i]+". Police affiliation NOT established by the supplied listing.","","User screenshot #1; page last updated 2019-06-18. Not independently verified."));
        ENTRIES=Collections.unmodifiableList(e);
    }
    public static List<Entry> entries(){return ENTRIES;}
}
