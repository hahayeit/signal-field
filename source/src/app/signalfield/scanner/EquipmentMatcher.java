package app.signalfield.scanner;

import java.util.*;

/** Vendor-assigned identifiers only; not a device, vehicle or operator classifier. */
public final class EquipmentMatcher {
    public static final String SOURCE="https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf";
    public static final String RULE_VERSION="2026-11-04.1";
    public static final String LIMITATION="Possible law-enforcement equipment only. Broadcast identifiers can be spoofed and do not confirm a police officer, vehicle, device model or distance. No matches does not mean no police nearby.";
    public static List<String> match(int[] companyIds,Collection<String> services){
        LinkedHashSet<String> reasons=new LinkedHashSet<>();
        if(companyIds!=null)for(int id:companyIds)if(id==0x034D)reasons.add("Manufacturer company ID 0x034D — TASER International, Inc.");
        if(services!=null)for(String service:services){
            if(service==null)continue;
            String value=service.toLowerCase(Locale.ROOT);
            if(value.equals("0000fc81-0000-1000-8000-00805f9b34fb"))reasons.add("Service UUID 0xFC81 — Axon Enterprise, Inc.");
            if(value.equals("0000fe6b-0000-1000-8000-00805f9b34fb"))reasons.add("Service UUID 0xFE6B — TASER International, Inc.");
            if(value.equals("0000fe6c-0000-1000-8000-00805f9b34fb"))reasons.add("Service UUID 0xFE6C — TASER International, Inc.");
        }
        return new ArrayList<>(reasons);
    }
    public static boolean recent(boolean scanning,long nowNanos,long receivedNanos){
        long age=nowNanos-receivedNanos;
        return scanning&&receivedNanos>0&&age>=0&&age<=30000000000L;
    }
}
