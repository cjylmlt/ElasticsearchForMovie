package util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageUtil {
    public static Map<String,String> xmlToMap(String xmlContent){
        Map<String, String> map = new HashMap<String,String>();
        SAXReader reader = new SAXReader();
        Document doc = null;
        try {
            doc = DocumentHelper.parseText(xmlContent);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        List<Element> list = root.elements();
        for(Element e:list)
            map.put(e.getName(), e.getText());
        return map;
    }
}
