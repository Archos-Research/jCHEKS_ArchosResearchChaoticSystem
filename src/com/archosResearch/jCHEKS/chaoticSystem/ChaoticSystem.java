package com.archosResearch.jCHEKS.chaoticSystem;

import com.archosResearch.jCHEKS.chaoticSystem.exception.*;
import com.archosResearch.jCHEKS.concept.chaoticSystem.AbstractChaoticSystem;
import com.archosResearch.jCHEKS.concept.exception.ChaoticSystemException;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import org.xml.sax.InputSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/**
 *
 * @author jean-francois
 */
public class ChaoticSystem extends AbstractChaoticSystem implements Cloneable {

    protected HashMap<Integer, Agent> agents = new HashMap();
    protected int lastGeneratedKeyIndex;
    protected AbstractChaoticSystem currentClone;
    protected byte[] toGenerateKey;
    protected int toGenerateKeyIndex;
    
    private int impactRangeMax;
    protected Range impactRange;
    protected Range keyPartRange;
    protected Range delayRange;
    
    private static final String XML_CHAOTICSYSTEM_NAME = "cs";
    private static final String XML_SYSTEMID_NAME = "si";
    private static final String XML_KEYLENGTH_NAME = "kl";
    private static final String XML_LASTKEY_NAME = "lk";
    private static final String XML_AGENTS_NAME = "as";    
    private static final String XML_KEYPART_RANGE_NAME = "kpr";
    private static final String XML_RANGE_MIN_NAME = "min";
    private static final String XML_RANGE_MAX_NAME = "max";
    
    private static final String XML_IMPACT_RANGE_NAME = "ir";
    private static final String XML_DELAY_RANGE_NAME = "dr";

    public HashMap<Integer, Agent> getAgents() {
        return this.agents;
    }

    protected ChaoticSystem() {}
    
    public ChaoticSystem(int keyLength, String systemId, Range impactRange, Range keyPartRange, Range delayRange, Random random) throws KeyLenghtException {
        this.keyLength = keyLength;
        this.systemId = systemId;
        
        this.impactRange = impactRange;
        this.impactRangeMax = impactRange.getMax();
        this.keyPartRange = keyPartRange;
        this.delayRange = delayRange;
        
        this.generateSystem(this.keyLength, random);
    }
        
    @Override
    public void evolveSystem(int factor) {
        
        for(Integer agentID : this.agents.keySet())
        {
            Agent a = this.agents.get(agentID);
            a.sendImpacts(this);
        }
        /*
        this.agents.entrySet().stream().forEach((a) -> {
            ((Agent) a.getValue()).sendImpacts(this);
        });*/

        for(Integer agentID : this.agents.keySet())
        {
            Agent a = this.agents.get(agentID);
            a.evolve(factor, this.impactRangeMax);
        }
        /*this.agents.entrySet().stream().forEach((a) -> {
            ((Agent) a.getValue()).evolve(factor, this.impactRange.getMax());
        });*/
        
        this.buildKey();//TODO execute buildKey when first calling getKey????
        this.currentClone = null;
        this.lastGeneratedKeyIndex = 0;

    }

    @Override
    public byte[] getKey(int requiredBitLength) throws KeyLenghtException, KeyGenerationException{
        if (requiredBitLength % Byte.SIZE == 0) {
            return generateByteKey(requiredBitLength / Byte.SIZE);
        }
        throw new KeyLenghtException("Invalid key length. Must be a multiple of 8.");
    }
    
    @Override
    public void resetSystem() {
        // TODO : Demander à François ce qu'il voyait là-dedans!
        //TODO FG: I think the idea is to revert to the system before cloning...
    }

    @Override
    public ChaoticSystem clone() throws CloneNotSupportedException {
        ChaoticSystem chaoticSystemClone = (ChaoticSystem) super.clone();
        
        chaoticSystemClone.agents = new HashMap();
        for (Map.Entry<Integer, Agent> entrySet : this.agents.entrySet()) {
            Integer key = entrySet.getKey();
            Agent value = entrySet.getValue();
            chaoticSystemClone.agents.put(key, (Agent) value.clone());
        }
        return chaoticSystemClone;
    }

    @Override
    public ChaoticSystem cloneSystem() throws CloningException {
        try {
            return this.clone();
        } catch (CloneNotSupportedException ex) {
            throw new CloningException("Unable to clone system.", ex);
        }

    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();

        sb.append(this.systemId);
        sb.append("!");
        sb.append(String.valueOf(this.keyLength));
        sb.append("!");
        sb.append(Utils.ByteArrayToString(this.lastGeneratedKey));
        sb.append("!");
        sb.append(Integer.toString(this.keyPartRange.getMin()));
        sb.append(":");
        sb.append(Integer.toString(this.keyPartRange.getMax()));
        sb.append("!");
        sb.append(Integer.toString(this.impactRange.getMin()));
        sb.append(":");
        sb.append(Integer.toString(this.impactRange.getMax()));
        sb.append("!");
        sb.append(Integer.toString(this.delayRange.getMin()));
        sb.append(":");
        sb.append(Integer.toString(this.delayRange.getMax()));
        sb.append("!");

        this.agents.entrySet().forEach((a) -> {
            sb.append("A");
            sb.append(((Agent) a.getValue()).serialize());
        });

        return sb.toString();
    }

    @Override
    public void deserialize(String serialization) {
        String[] values = serialization.split("!");

        this.systemId = values[0];
        this.keyLength = Integer.parseInt(values[1]);
        this.lastGeneratedKey = Utils.StringToByteArray(values[2]);
        
        String[] minMaxKey = values[3].split(":");
        this.keyPartRange = new Range(Integer.parseInt(minMaxKey[0]), Integer.parseInt(minMaxKey[1]));
        
        String[] minMaxImpact = values[4].split(":");
        this.impactRange = new Range(Integer.parseInt(minMaxImpact[0]), Integer.parseInt(minMaxImpact[1]));
        
        String[] minMaxDelay = values[5].split(":");
        this.delayRange = new Range(Integer.parseInt(minMaxDelay[0]), Integer.parseInt(minMaxDelay[1])); 
        
        this.agents = new HashMap();
        String[] agentValues = values[6].substring(1).split("A");
        for (String agentString : agentValues) {
            Agent tempAgent = new Agent(agentString, this.keyPartRange);
            this.agents.put(tempAgent.getAgentId(), tempAgent);
        }
    }

    public static ChaoticSystem deserializeXML(String xml) throws Exception {

        ChaoticSystem system = new ChaoticSystem();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = dBuilder.parse(is);
        
        doc.getDocumentElement().normalize();
        system.systemId = doc.getElementsByTagName(XML_SYSTEMID_NAME).item(0).getTextContent();
        system.keyLength = Integer.parseInt(doc.getElementsByTagName(XML_KEYLENGTH_NAME).item(0).getTextContent());

        system.lastGeneratedKey = Utils.StringToByteArray(doc.getElementsByTagName(XML_LASTKEY_NAME).item(0).getTextContent());
       
        Element keyPartRange = (Element) doc.getElementsByTagName(XML_KEYPART_RANGE_NAME).item(0);
        int min = Integer.parseInt(keyPartRange.getElementsByTagName(XML_RANGE_MIN_NAME).item(0).getTextContent());
        int max = Integer.parseInt(keyPartRange.getElementsByTagName(XML_RANGE_MAX_NAME).item(0).getTextContent());
        system.keyPartRange = new Range(min, max);
        
        Element impactRange = (Element) doc.getElementsByTagName(XML_IMPACT_RANGE_NAME).item(0);
        min = Integer.parseInt(impactRange.getElementsByTagName(XML_RANGE_MIN_NAME).item(0).getTextContent());
        max = Integer.parseInt(impactRange.getElementsByTagName(XML_RANGE_MAX_NAME).item(0).getTextContent());
        system.impactRange = new Range(min, max); 
        
        Element delayRange = (Element) doc.getElementsByTagName(XML_DELAY_RANGE_NAME).item(0);
        min = Integer.parseInt(delayRange.getElementsByTagName(XML_RANGE_MIN_NAME).item(0).getTextContent());
        max = Integer.parseInt(delayRange.getElementsByTagName(XML_RANGE_MAX_NAME).item(0).getTextContent());
        system.delayRange = new Range(min, max);
        
        NodeList nList = doc.getElementsByTagName(Agent.XML_AGENT_NAME);
        system.agents = new HashMap();

        for(int i = 0; i < nList.getLength(); i++) {
            Node element = nList.item(i);
            Agent tempAgent = new Agent((Element) element, system.keyPartRange);
            system.agents.put(tempAgent.getAgentId(), tempAgent);
        }
        
        system.buildKey();
        
        return system;
    }

    public String serializeXML() throws TransformerConfigurationException, TransformerException, Exception {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement(XML_CHAOTICSYSTEM_NAME);
            doc.appendChild(rootElement);

            Element systemIdElement = doc.createElement(XML_SYSTEMID_NAME);
            systemIdElement.appendChild(doc.createTextNode(this.systemId));
            rootElement.appendChild(systemIdElement);

            Element keyLengthElement = doc.createElement(XML_KEYLENGTH_NAME);
            keyLengthElement.appendChild(doc.createTextNode(Integer.toString(this.keyLength)));
            rootElement.appendChild(keyLengthElement);

            Element lastKey = doc.createElement(XML_LASTKEY_NAME);
            lastKey.appendChild(doc.createTextNode(Utils.ByteArrayToString(this.lastGeneratedKey)));
            rootElement.appendChild(lastKey);

            Element keyPartRangeElement = doc.createElement(XML_KEYPART_RANGE_NAME);
            Element min = doc.createElement(XML_RANGE_MIN_NAME);
            Element max = doc.createElement(XML_RANGE_MAX_NAME);
            min.appendChild(doc.createTextNode(Integer.toString(this.keyPartRange.getMin())));
            max.appendChild(doc.createTextNode(Integer.toString(this.keyPartRange.getMax())));
            keyPartRangeElement.appendChild(min);
            keyPartRangeElement.appendChild(max);        
            rootElement.appendChild(keyPartRangeElement);
            
            Element impactRangeElement = doc.createElement(XML_IMPACT_RANGE_NAME);
            min = doc.createElement(XML_RANGE_MIN_NAME);
            max = doc.createElement(XML_RANGE_MAX_NAME);
            min.appendChild(doc.createTextNode(Integer.toString(this.impactRange.getMin())));
            max.appendChild(doc.createTextNode(Integer.toString(this.impactRange.getMax())));
            impactRangeElement.appendChild(min);
            impactRangeElement.appendChild(max);        
            rootElement.appendChild(impactRangeElement);
            
            Element delayRangeElement = doc.createElement(XML_DELAY_RANGE_NAME);
            min = doc.createElement(XML_RANGE_MIN_NAME);
            max = doc.createElement(XML_RANGE_MAX_NAME);
            min.appendChild(doc.createTextNode(Integer.toString(this.delayRange.getMin())));
            max.appendChild(doc.createTextNode(Integer.toString(this.delayRange.getMax())));
            delayRangeElement.appendChild(min);
            delayRangeElement.appendChild(max);        
            rootElement.appendChild(delayRangeElement);
            
            Element agentsElement = doc.createElement(XML_AGENTS_NAME);

            for(int i = 0; i < this.agents.size(); i++) {
                agentsElement.appendChild(this.agents.get(i).serializeXml(rootElement));
            }
            /*this.agents.entrySet().forEach((a) -> {
                agentsElement.appendChild(a.getValue().serializeXml(rootElement));
            });*/

            rootElement.appendChild(agentsElement);
            
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            //StreamResult result = new StreamResult(new File("system\temp.xml"));
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            
            return writer.toString();
        } catch (ParserConfigurationException ex) {
            throw new XMLSerializationException("Error serializing XML", ex);
        }
    }

    @Override
    public final void generateSystem(int keyLength, Random random) throws KeyLenghtException{
        this.keyLength = keyLength;

        if ((this.keyLength % 8) != 0) {
            throw new KeyLenghtException("Invalid key length. Must be a multiple of 128.");
        }

        //TODO We might want another extra for the cipherCheck
        int numberOfAgents = this.keyLength / Byte.SIZE;
        for (int i = 0; i < numberOfAgents; i++) {
            //this.agents.put(i, new Agent(i, this.maxImpact, numberOfAgents, numberOfAgents - 1, random));
            this.agents.put(i, new Agent(i, this.impactRange, this.keyPartRange, this.delayRange, numberOfAgents, numberOfAgents - 1, random));
        }

        this.buildKey();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.agents);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final ChaoticSystem other = (ChaoticSystem) obj;
        if (!Objects.equals(this.agents, other.agents)) return false;
        return true;
    }
    
    @Override
    public boolean isSameState(AbstractChaoticSystem other) {
        ChaoticSystem otherSystem = (ChaoticSystem)other;
        
        Set<Integer> agentIDs = this.agents.keySet();
        for (Integer agentID : agentIDs) {
            Agent agent = this.agents.get(agentID);
            if (!otherSystem.agents.get(agentID).isSameState(agent)) return false;
        }
        return true;
    }
    
    private byte[] generateByteKey(int requiredByteLength) throws KeyGenerationException {
        try {
            this.toGenerateKey = new byte[requiredByteLength];
            if (requiredByteLength >= this.lastGeneratedKey.length - this.lastGeneratedKeyIndex) {

                this.toGenerateKeyIndex = 0;
                setClone();
                pickCloneKey();
                while (this.toGenerateKeyIndex < requiredByteLength) {
                    fillKey();
                    if (this.toGenerateKeyIndex < requiredByteLength - 1) {
                        evolveClone();
                    }
                }

            } else {
                copyBytesFromLastToNextKey(requiredByteLength);
                this.lastGeneratedKeyIndex += requiredByteLength;
            }
            return this.toGenerateKey;
        } catch (CloningException ex) {
            throw new KeyGenerationException("Error in key generation process.", ex);
        }
    }

    private void copyBytesFromLastToNextKey(int numberOfBytesToCopy) {
        System.arraycopy(this.lastGeneratedKey, lastGeneratedKeyIndex, this.toGenerateKey, this.toGenerateKeyIndex, numberOfBytesToCopy);
    }

    private void fillKey() {
        int numberOfByteToCopy = getNumberOfByteToCopy(this.toGenerateKey.length, this.toGenerateKeyIndex);
        copyBytesFromLastToNextKey(numberOfByteToCopy);
        this.toGenerateKeyIndex += numberOfByteToCopy;
        this.lastGeneratedKeyIndex += numberOfByteToCopy;
    }

    private int getNumberOfByteToCopy(int requiredLength, int fullKeyIndex) {
        int numberOfMissingBytes = requiredLength - fullKeyIndex;
        return (this.lastGeneratedKey.length - this.lastGeneratedKeyIndex > numberOfMissingBytes) ? numberOfMissingBytes : this.lastGeneratedKey.length - lastGeneratedKeyIndex;
    }

    private void evolveClone() {
        try {
            try {
                this.currentClone.evolveSystem();
            } catch (Exception ex) {
                Logger.getLogger(ChaoticSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.lastGeneratedKey = this.currentClone.getKey();
            this.lastGeneratedKeyIndex = 0;
        } catch (ChaoticSystemException ex) {
            Logger.getLogger(ChaoticSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void pickCloneKey() {
        try {
            this.lastGeneratedKey = this.currentClone.getKey();
        } catch (ChaoticSystemException ex) {
            Logger.getLogger(ChaoticSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setClone() throws CloningException {
        this.currentClone = (this.currentClone == null) ? cloneSystem() : this.currentClone;
    }
    
    protected void buildKey() {
        this.lastGeneratedKey = new byte[(this.keyLength / Byte.SIZE)];

        for (int i = 0; i < (this.keyLength / Byte.SIZE); i++) {
            this.lastGeneratedKey[i] = ((Agent) this.agents.get(i)).getKeyPart();
        }
    }

    @Override
    public int getAgentsCount() {
        return this.agents.size();
    }
}
