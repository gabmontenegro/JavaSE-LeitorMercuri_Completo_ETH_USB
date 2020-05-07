/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.transire;

import com.thingmagic.ReadListener;
import com.thingmagic.Reader;
import com.thingmagic.ReaderException;
import com.thingmagic.SimpleReadPlan;
import com.thingmagic.TMConstants;
import com.thingmagic.TagData;
import com.thingmagic.TagProtocol;
import com.thingmagic.TagReadData;
import com.thingmagic.TransportListener;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author gabriel montenegro villacrez
 */
public class ItbamLeitorUSB implements ReadListener {

    static TransportListener currentListener;
    boolean trace = true;

    public Reader r = null;
    public int[] antennaList = {1};
    public int ntags = 1;
    PortalRFID lg;

    public ItbamLeitorUSB(PortalRFID aux) {
        this.lg = aux;
    }

    public void Desconectar() throws ReaderException {
        
        this.r.removeReadListener(this);
        this.r.stopReading();
        this.r.reboot();
        this.r.destroy();
        lg.log("Leitor USB foi desconectado!");
    }

    public static void setTrace(Reader r, String args[]) {
        if (args[0].toLowerCase().equals("on")) {
            r.addTransportListener(Reader.simpleTransportListener);
            TransportListener currentListener = Reader.simpleTransportListener;
        } else if (currentListener != null) {
            r.removeTransportListener(Reader.simpleTransportListener);
        }
    }

    public void Conectar() throws ReaderException 
    {

        try {
            r = Reader.create("tmr:///com12");

            if (trace) {
                setTrace(r, new String[]{"on"});
            }
            
            r.connect();
            lg.log("Leitor USB Conectado!");
        } catch (ReaderException e) {
            JOptionPane.showMessageDialog(null, "Erro de conexao: " + e.getMessage());
        }
        if (Reader.Region.UNSPEC == (Reader.Region) r.paramGet("/reader/region/id")) {
            Reader.Region[] supportedRegions = (Reader.Region[]) r.paramGet(TMConstants.TMR_PARAM_REGION_SUPPORTEDREGIONS);
            if (supportedRegions.length < 1) {
                lg.log("Reader doesn't support any regions");
            } else {
                r.paramSet("/reader/region/id", supportedRegions[0]);
            }
        }

        String model = r.paramGet("/reader/version/model").toString();
        Boolean checkPort = (Boolean) r.paramGet(TMConstants.TMR_PARAM_ANTENNA_CHECKPORT);
        String swVersion = (String) r.paramGet(TMConstants.TMR_PARAM_VERSION_SOFTWARE);
        if ((model.equalsIgnoreCase("M6e Micro") || model.equalsIgnoreCase("M6e Nano")
                || (model.equalsIgnoreCase("Sargas") && (swVersion.startsWith("5.1")))) && (false == checkPort) && antennaList == null) {
            lg.log("Module doesn't has antenna detection support, please provide antenna list");
            r.destroy();

        }
        
        //lg.log("RADIO POWER" + r.paramGet(TMConstants.TMR_PARAM_RADIO_READPOWER) );
        // System.out.println("ASYNC TIME ON: " + r.paramGet(TMConstants.TMR_PARAM_READ_ASYNCONTIME) );
        //   System.out.println("ASYNC TIME OFF: " + r.paramGet(TMConstants.TMR_PARAM_READ_ASYNCOFFTIME));
        //r.paramSet(TMConstants.TMR_PARAM_RADIO_READPOWER, 3000);
        SimpleReadPlan plan = new SimpleReadPlan(antennaList, TagProtocol.GEN2, null, null, 1000);
        r.paramSet(TMConstants.TMR_PARAM_READ_PLAN, plan);
        
        r.addReadListener(this);
        r.startReading();
        lg.log("Inicando leitura USB!");

    }
    
    HashSet<TagData> seenTags = new HashSet<TagData>();
    @Override
    public void tagRead(Reader r, TagReadData t) {
        //lg.log(String.format("EPC:%s", (t == null) ? "none" : t.epcString()));
        //lg.publish("T1", 2, t.epcString().getBytes());
        TagData tr = t.getTag();
        if (!seenTags.contains(tr))
        {
            lg.log("#" + ntags + " \t EPC: " + tr.epcString());
            //lg.publish("T1", 2, tr.epcString().getBytes());
            seenTags.add(tr);
            ntags++;
        }
        // System.out.print(t.toString());
    }
    
    public void zerarHashSet()
    {
        seenTags.clear();
        ntags = 1;
    }
}
