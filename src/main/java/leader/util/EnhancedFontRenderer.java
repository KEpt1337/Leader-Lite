package leader.util;

import leader.util.optimize.CachedString;
import leader.util.optimize.StringHash;
import net.minecraft.client.renderer.GLAllocation;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class EnhancedFontRenderer {

    private static final List<EnhancedFontRenderer> instances = new ArrayList<>();
    private final List<StringHash> obfuscated = new ArrayList<>();
    private final Map<String, Integer> stringWidthCache = new HashMap<>();
    private final Queue<Integer> glRemoval = new ConcurrentLinkedQueue<>();
    private final Map<StringHash, CachedString> stringCache = new HashMap<>();

    public EnhancedFontRenderer() {
        instances.add(this);
    }

    public static List<EnhancedFontRenderer> getInstances() {
        return instances;
    }

    public String getName() {
        return "Enhanced Font Renderer";
    }

    public void tick() {for (StringHash hash : obfuscated) {
            stringCache.remove(hash);
        }
        obfuscated.clear();
    }

    public int getGlList() {
        final Integer poll = glRemoval.poll();
        return poll == null ? GLAllocation.generateDisplayLists(1) : poll;
    }

    public CachedString get(StringHash key) {
        return stringCache.get(key);
    }

    public void cache(StringHash key, CachedString value) {
        int maxCacheSize = 5000;
        if (stringCache.size() >= maxCacheSize) {
            stringCache.clear();
        }
        stringCache.put(key, value);
    }

    public Map<String, Integer> getStringWidthCache() {
        return stringWidthCache;
    }

    public void invalidateAll() {
        this.stringCache.clear();
    }

    public List<StringHash> getObfuscated() {
        return obfuscated;
    }






}



