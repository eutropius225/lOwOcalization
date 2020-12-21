package eutros.lowocalization.core.common;

import eutros.lowocalization.api.ILOwOConfigurableTransformation;
import eutros.lowocalization.api.ILOwOTransformation;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LOwODefaultTransformations {

    public static List<ILOwOConfigurableTransformation> getAll() {
        LinkedList<ILOwOConfigurableTransformation> defaults = new LinkedList<>();
        defaults.add(patternBased(Pattern.compile("([\"'])(?<from>.+)\\1->([\"'])(?<to>.+)\\3"), LOwODefaultTransformations::makeLiteral));
        defaults.add(patternBased(Pattern.compile("s(.)(?<pattern>.*?[^\\\\])\\1(?<replace>.*)\\1(?<flags>\\w*)"), LOwODefaultTransformations::makeRegex));
        defaults.add(s -> s.startsWith("__asm__ ") ? LOwOAssembly.make(s.substring("__asm__ ".length())) : Optional.empty());
        return defaults;
    }

    private static ILOwOConfigurableTransformation patternBased(Pattern pattern, Function<Matcher, ILOwOTransformation> mapper) {
        return configuration -> Optional.of(pattern.matcher(configuration))
                .filter(Matcher::matches)
                .map(mapper);
    }

    private static ILOwOTransformation makeLiteral(Matcher matcher) {
        String from = matcher.group("from");
        String to = matcher.group("to");
        if(from.length() == 1 && to.length() == 1) {
            char fromc = from.charAt(0);
            char toc = to.charAt(0);
            return s -> s.replace(fromc, toc);
        }
        return s -> s.replace(from, to);
    }

    private static ILOwOTransformation makeRegex(Matcher matcher) {
        String pattern = matcher.group("pattern");
        String replace = matcher.group("replace");
        String flags = matcher.group("flags");
        boolean global = flags.contains("g");
        if(!flags.isEmpty()) {
            if(global) flags = flags.replace("g", "");
            pattern = String.format("(?%s)%s", flags, pattern);
        }
        Pattern compiled = Pattern.compile(pattern);
        if (global) {
            return source -> compiled.matcher(source).replaceAll(replace);
        } else {
            return source -> compiled.matcher(source).replaceFirst(replace);
        }
    }

}
