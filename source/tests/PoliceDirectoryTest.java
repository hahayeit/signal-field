import app.signalfield.scanner.PoliceDirectory;
public class PoliceDirectoryTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static int count(int category,String query){int n=0;for(PoliceDirectory.Entry e:PoliceDirectory.entries())if(e.matches(category,query))n++;return n;}
    public static void main(String[] args){
        check(count(0,"")==6,"police includes 4 dispatch TGIDs and 2 sheriff conventional channels");
        check(count(1,"")==12,"Monroe site has 12 shared channels");
        check(count(2,"")==2,"corrections isolated");
        check(count(3,"")==3,"commercial listing remains unverified");
        check(count(4,"")==23,"all local reference entries");
        check(count(0,"774.31875")==0,"control channels are not police-specific");
        check(count(1,"774.31875")==1,"control channel discoverable in shared site");
        check(count(0,"451.7875")==0,"commercial frequency not classified as police");
        check(count(0,"155.9025")==0,"corrections not dispatch");
        check(count(0,"  MONROE POLICE ")==2,"case-insensitive trimmed agency search");
        check(count(0,"53050")==1,"talkgroup ID searchable");
        check(count(4,"no matching entry")==0,"empty results");
        for(PoliceDirectory.Entry e:PoliceDirectory.entries())if(e.value.equals("53050")){check(e.displayValue().equals("TGID 53050"),"TGID is not MHz");check(e.details.contains("D enc"),"retain source encryption notation");}
        System.out.println("Police directory: 14 checks passed");
    }
}
