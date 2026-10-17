package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VpParser {

    // Qualifiers são os MAPs
    public static class Qualifier {
        String name;
        String type;

        public Qualifier(String name, String type) {
            this.name = name;
            this.type = type;
        }


        @Override
        public String toString() {
            return "Qualifier {" +
                    "name:'" + name + '\'' +
                    ", type:'" + type + '\'' +
                    '}';
        }
    }


    /**
     * Para Extrair os qualifiers, SD metrics nao consegue direito porque está muito nested e nao possui ids
     * <ownedEnd> -> <qualifier> -> <modelRef value="id"/> -> <packagedElement xmi:id="id">
     * Entao faz se "manual" a leitura
     */

    public static Map<String, Qualifier> parseQualifiers(String filePath) throws Exception {
        Map<String, Qualifier> qualifiers = new HashMap<>();

        // extrai todos packagedElements que tenham name type e id
        // <packagedElement ... name="numero" type="Integer_id" ... xmi:id="7mrpMTmGAqACRwgl" ...>
        Pattern elementPattern = Pattern.compile(
                "<packagedElement[^>]+name=\"([^\"]+)\"[^>]+type=\"([^\"]+)\"[^>]+xmi:id=\"([^\"]+)\"");

        //  XMI ID -> Qualifier
        Map<String, Qualifier> elementsById = new HashMap<>();

        // cria todos os possiveis qualifiers (chaves)
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var matcher = elementPattern.matcher(line);
                if (matcher.find()) {
                    String name = matcher.group(1);
                    String type = matcher.group(2);
                    String id = matcher.group(3);

                    String fixedType = fixTypeName(type);

                    Qualifier qualifier = new Qualifier(name, fixedType);
                    elementsById.put(id, qualifier);
                }
            }
        }

        Pattern modelRefPattern = Pattern.compile("<modelRef value=\"([^\"]+)\"/>");
        // se não-navegável aparece como <ownedEnd> (pertence à associação).
        // se a ponta é navegável aparece como <ownedAttribute> (pertence à classe)
        // aparece nos dois mas so um tem info
        Pattern ownedEndPattern = Pattern.compile("<(?:ownedEnd|ownedAttribute)[^>]+xmi:id=\"([^\"]+)\"");

        // agora percorre as pontas desses qualifiers e ve se tem <qualifier>
        // se sim pega o <modelRef> e ve se é um dos ids obtidos em cima, se sim é um qualifier real
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String currentOwnedEndId = null;
            boolean hasQualifier = false;
            while ((line = reader.readLine()) != null) {
                // Detetar owned
                var ownedEndMatcher = ownedEndPattern.matcher(line);
                if (ownedEndMatcher.find()) {
                    currentOwnedEndId = ownedEndMatcher.group(1);
                }

                // detetar qualifier (nao quer dizer que tenha) porque existem qualifiers que estão so vazios
                if (line.contains("<qualifier>")) {
                    hasQualifier = true;
                }

                // dentro do <qualifier> de uma ponta: o <modelRef> aponta para o atributo chave
                if (hasQualifier && currentOwnedEndId != null) {
                    Matcher modelRefMatcher = modelRefPattern.matcher(line);
                    if (modelRefMatcher.find()) {
                        String refId = modelRefMatcher.group(1);

                        // ve o id e procura nas chaves obtidas em cima se existir é um qualifier real
                        Qualifier qualifier = elementsById.get(refId);
                        if (qualifier != null) {
                            qualifiers.put(currentOwnedEndId, qualifier);
                        }
                    }
                }

                // detetar fim qualifier
                if (line.contains("</qualifier>")) {
                    hasQualifier = false;
                }

                // detetar fim da ponta (ownedEnd ou ownedAttribute)
                if (line.contains("</ownedEnd>") || line.contains("</ownedAttribute>")) {
                    currentOwnedEndId = null;
                }
            }
            return qualifiers;
        }
    }

    private static String fixTypeName(String typeId) {
        if (typeId != null && typeId.endsWith("_id")) {
            return typeId.substring(0, typeId.length() - 3);
        }

        return typeId != null ? typeId : "Object";
    }


    /**
     * extrai o numero de visualização do VP (<number xmi:value="1.1.2"/>) de cada mensagem.
     * SDMetrics nao consegue extrair direito porque ha varios xmi:value diferentes dentro de <xmi:Extension>.
     */
    public static Map<String, String> extractMessageNumbers(String filePath) throws IOException {
        Map<String, String> idToNumber = new HashMap<>();

        Pattern messagePattern = Pattern.compile("<message[^>]+xmi:id=\"([^\"]+)\"");
        Pattern numberPattern = Pattern.compile("<number xmi:value=\"([^\"]+)\"/>");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String currentMessageId = null;
            boolean captured = false;

            while ((line = reader.readLine()) != null) {
                // detetar inicio de mensagem
                Matcher messageMatcher = messagePattern.matcher(line);
                if (messageMatcher.find()) {
                    currentMessageId = messageMatcher.group(1);
                    captured = false;
                }

                // detetar number dentro da mensagem (so o primeiro)
                if (currentMessageId != null && !captured) {
                    Matcher numberMatcher = numberPattern.matcher(line);
                    if (numberMatcher.find()) {
                        idToNumber.put(currentMessageId, numberMatcher.group(1));
                        captured = true;
                    }
                }

                // detetar fim de mensagem
                if (line.contains("</message>")) {
                    currentMessageId = null;
                    captured = false;
                }
            }
        }

        return idToNumber;
    }
}
