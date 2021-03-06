package com.archosResearch.jCHEKS.chaoticSystem;

import java.io.Serializable;
import java.util.Random;
import org.w3c.dom.*;

/**
 *
 * @author jean-francois
 */
public class Rule implements Cloneable, Serializable{

    protected int destination;
    protected int impact;
    protected int delay;
    
    public static final String XML_RULE_NAME = "r";
    private static final String XML_DESTINATION_NAME = "d";
    private static final String XML_DELAY_NAME = "dl";
    private static final String XML_IMPACT_NAME = "i";

    
    public int getDestination() {
        return this.destination;
    }
    
    public int getImpact() {
        return this.impact;
    }
    
    public int getDelay() {
        return this.delay;
    }
    
    public Rule(int destination, Range impactRange, Range delayRange, Random random) {
        this.destination = destination;
        this.impact = Utils.GetRandomIntAvoidingZero(impactRange, random);
        this.delay = Utils.GetRandomInt(delayRange, random);
    }
    
    protected Rule() {}

    public Rule(String serialization)
    {
        String[] values = serialization.split("%");
        
        this.destination = Integer.parseInt(values[0]);
        this.impact = Integer.parseInt(values[1]);
        this.delay = Integer.parseInt(values[2]);
    }
    
    public Rule(Element element) {
        this.destination = Integer.parseInt(element.getElementsByTagName(XML_DESTINATION_NAME).item(0).getTextContent());
        this.impact = Integer.parseInt(element.getElementsByTagName(XML_IMPACT_NAME).item(0).getTextContent());
        this.delay = Integer.parseInt(element.getElementsByTagName(XML_DELAY_NAME).item(0).getTextContent());

    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rule other = (Rule) obj;
        if (this.destination != other.destination) {
            return false;
        }
        if (this.impact != other.impact) {
            return false;
        }
        if (this.delay != other.delay) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.destination;
        hash = 47 * hash + this.impact;
        hash = 47 * hash + this.delay;
        return hash;
    }
    
    @Override
    public Rule clone() throws CloneNotSupportedException{
        return (Rule)super.clone();
    }
    
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.valueOf(this.destination));
        sb.append("%");
        sb.append(String.valueOf(this.impact));
        sb.append("%");
        sb.append(String.valueOf(this.delay));
        
        return sb.toString();
    }
    
    public Element serializeXml(Element root) {
            
        Document doc = root.getOwnerDocument();
        Element rootElement = doc.createElement(XML_RULE_NAME);

        Element systemIdElement = doc.createElement(XML_DESTINATION_NAME);
        systemIdElement.appendChild(doc.createTextNode(Integer.toString(this.destination)));
        rootElement.appendChild(systemIdElement);

        Element keyLengthElement = doc.createElement(XML_IMPACT_NAME);
        keyLengthElement.appendChild(doc.createTextNode(Integer.toString(this.impact)));
        rootElement.appendChild(keyLengthElement);

        Element lastKey = doc.createElement(XML_DELAY_NAME);
        lastKey.appendChild(doc.createTextNode(Integer.toString(this.delay)));
        rootElement.appendChild(lastKey);

        return rootElement;
    }
}
