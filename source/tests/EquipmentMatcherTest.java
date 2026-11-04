import app.signalfield.scanner.EquipmentMatcher;
import java.util.*;

public class EquipmentMatcherTest {
    static int checks=0;
    static void check(boolean b,String description){checks++;if(!b)throw new AssertionError(description);}
    static List<String> match(int[] ids,String... uuids){return EquipmentMatcher.match(ids,Arrays.asList(uuids));}
    static String uuid(String shortId){return "0000"+shortId+"-0000-1000-8000-00805f9b34fb";}
    public static void main(String[] args){
        check(EquipmentMatcher.match(null,null).isEmpty(),"missing fields do not match");
        check(match(new int[]{0x004c,0x0006}).isEmpty(),"common unrelated manufacturers do not match");
        check(match(new int[]{0x034d}).size()==1,"TASER company ID");
        check(match(new int[]{0x034d,0x034d}).size()==1,"duplicate manufacturer reason removed");
        check(match(new int[]{0xfc81,0xfe6b,0xfe6c}).isEmpty(),"service UUID values not treated as company IDs");
        check(match(null,uuid("034d")).isEmpty(),"company ID value not treated as service UUID");
        check(match(null,uuid("fc81")).size()==1,"Axon full service UUID");
        check(match(null,uuid("fe6b")).size()==1,"TASER FE6B service");
        check(match(null,uuid("fe6c").toUpperCase(Locale.ROOT)).size()==1,"uppercase UUID canonicalization");
        check(match(null,uuid("fe6d"),uuid("180f")).isEmpty(),"neighboring service and battery service do not match");
        check(match(null,"0000fc81-1111-1000-8000-00805f9b34fb").isEmpty(),"custom UUID containing token is not a match");
        check(match(null,"AXON_CAMERA","TASER","fc81",null).isEmpty(),"names, substrings and null do not match");
        check(match(new int[]{0x034d},uuid("fc81"),uuid("fe6b"),uuid("fe6c")).size()==4,"all exact evidence retained");
        check(match(null,uuid("fc81"),uuid("fc81")).size()==1,"UUID appearing in service list and service data deduplicates");
        check(EquipmentMatcher.recent(true,30000000001L,1),"recent boundary inclusive");
        check(!EquipmentMatcher.recent(true,30000000002L,1),"old match goes stale");
        check(!EquipmentMatcher.recent(false,2,1),"stopped scans never show current presence");
        check(!EquipmentMatcher.recent(true,1,2),"future timestamp not recent");
        check(!EquipmentMatcher.recent(true,1,0),"unknown timestamp not recent");
        check(EquipmentMatcher.LIMITATION.contains("No matches does not mean no police"),"absence is not all-clear");
        System.out.println("Equipment matcher: "+checks+" checks passed");
    }
}
