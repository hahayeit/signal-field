import app.signalfield.scanner.SignalRules;
public class SignalRulesTest {
    static int checks=0;
    static void check(boolean condition,String message) { checks++; if(!condition)throw new AssertionError(message); }
    public static void main(String[] args) {
        check(SignalRules.hex(new byte[]{0,15,-1,-128}).equals("000FFF80"),"raw unsigned advertisement bytes");
        check(SignalRules.hex(null).equals(""),"missing advertisement");
        check(SignalRules.hex(new byte[0]).equals(""),"empty advertisement");
        check(SignalRules.strength(-55).equals("STRONG"),"strong threshold");
        check(SignalRules.strength(-56).equals("MODERATE"),"moderate threshold");
        check(SignalRules.strength(-70).equals("MODERATE"),"moderate lower boundary");
        check(SignalRules.strength(-71).equals("WEAK"),"weak threshold");
        check(SignalRules.freshFix(60000000000L,0),"location freshness boundary");
        check(!SignalRules.freshFix(60000000001L,0),"old location excluded from map tags");
        check(!SignalRules.freshFix(0,1),"future location rejected");
        check(SignalRules.freshWifi(15000001,1),"Wi-Fi freshness boundary");
        check(!SignalRules.freshWifi(15000002,1),"stale Wi-Fi excluded from observation log");
        check(!SignalRules.freshWifi(1000,0),"unknown Wi-Fi timestamp rejected");
        check(!SignalRules.freshWifi(1,2),"future Wi-Fi timestamp rejected");
        System.out.println(checks+" checks passed");
    }
}
