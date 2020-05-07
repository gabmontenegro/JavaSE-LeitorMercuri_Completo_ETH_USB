/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.transire;

import static br.com.transire.ItbamLeitorUSB.setTrace;
import com.thingmagic.*;
import java.util.HashSet;
import java.util.logging.Level;

/**
 *
 * @author gabriel montenegro villacrez
 */
public class ItbamLeitorEthernet implements ReadListener, ReadExceptionListener
{

    static String tcpURI;
    public Reader r = null;
    public int[] antennaList = {1, 2};
    public int ntagsLidas = 1;
    
    PortalRFID lg;
    
    HashSet<TagData> seenTags = new HashSet<TagData>();
   

    

    public ItbamLeitorEthernet(PortalRFID aux) {
        this.lg = aux;
        this.r = null;
    }
    
    
    synchronized void Desconectar() 
    {
     
        this.r.stopReading();
        lg.log("Leitura interrompida!");
        this.r.removeReadListener(this);
        this.r.destroy();
        lg.log("Leitor Ethernet desconectado !");

    }

    public void Conectar() throws ReaderException, Exception
    {

        try 
        {            
            Reader.setSerialTransport("tcp", new SerialTransportTCP.Factory());
            String readerURI = "tcp://192.168.0.100:5000";
            
            r = Reader.create(readerURI);

            setTrace(r, new String[]{"on"});
            
            r.connect();
            lg.log("Leitor Conectado!");
            if (Reader.Region.UNSPEC == (Reader.Region) r.paramGet("/reader/region/id"))
            {
                Reader.Region[] supportedRegions = (Reader.Region[]) r.paramGet(TMConstants.TMR_PARAM_REGION_SUPPORTEDREGIONS);
                if (supportedRegions.length < 1)
                {
                    throw new Exception("Reader doesn't support any regions");
                }
                else
                {
                    r.paramSet("/reader/region/id", supportedRegions[0]);
                }
            }

            String model = r.paramGet("/reader/version/model").toString();
            Boolean checkPort = (Boolean)r.paramGet(TMConstants.TMR_PARAM_ANTENNA_CHECKPORT);
            if ((model.equalsIgnoreCase("M6e Micro") || model.equalsIgnoreCase("M6e Nano") ||
                (false == checkPort)) && antennaList == null)
            {
                lg.log("Module doesn't has antenna detection support, please provide antenna list");
                r.destroy();

            }
            
            //lg.log("Read power default: " + r.paramGet(TMConstants.TMR_PARAM_RADIO_READPOWER));
            r.paramSet(TMConstants.TMR_PARAM_RADIO_READPOWER, 2000 );
            SimpleReadPlan plan = new SimpleReadPlan(antennaList, TagProtocol.GEN2, null, null, 1000);
            r.paramSet(TMConstants.TMR_PARAM_READ_PLAN, plan);

           // Create and add tag listener
            r.addReadListener(this);
            // search for tags in the background
            r.startReading();
            lg.log("Iniciando Leitura de Tags:");
       

        } catch (ReaderException e) {
            lg.log("[Exception]" + e.getMessage());

        }
    }
  
    @Override
    public void tagRead(Reader r, TagReadData tr)
    {
       //lg.publish("T1", 2, t.epcString().getBytes());
        TagData t = tr.getTag();
         
        if (!seenTags.contains(t)) {
            lg.log("# " + ntagsLidas + " Tag: " + t.epcString());
            ntagsLidas++;
            seenTags.add(t);

        }
    }
     
   public void zerarTagsLidas()
   {
       seenTags.clear();
       ntagsLidas = 1;
       
   }
   
    @Override
    public void tagReadException(Reader r, ReaderException re) {
        lg.log( "Exception na leitura de Tags: " + re.getMessage());
    }
}
  