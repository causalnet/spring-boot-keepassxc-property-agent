package au.net.causal.springboot.keepassxcpropertyagent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class AgentConfiguration
{
    private static final String DEFAULT_ENTRY_URI = "spring://app";

    private final List<String> entryUris = new ArrayList<>();

    public static AgentConfiguration parse(String argsString)
    {
        if (argsString == null)
        {
            argsString = "";
        }

        Map<String, String> argMap = new LinkedHashMap<>();

        for (String argSegment : argsString.split(Pattern.quote(",")))
        {
            String[] argSplit = argSegment.split(Pattern.quote("="), 2);
            if (argSplit.length > 1)
            {
                argMap.put(argSplit[0], argSplit[1]);
            }
        }

        AgentConfiguration args = new AgentConfiguration();
        if (argMap.containsKey("entryUri"))
        {
            args.addEntryUri(argMap.get("entryUri"));
        }

        if (args.getEntryUris().isEmpty())
            args.addEntryUri(DEFAULT_ENTRY_URI);


        return args;
    }

    public List<String> getEntryUris()
    {
        return entryUris;
    }

    public void addEntryUri(String entryUri)
    {
        entryUris.add(entryUri);
    }
}
