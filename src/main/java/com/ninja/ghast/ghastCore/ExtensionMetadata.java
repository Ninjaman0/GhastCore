package com.ninja.ghast.ghastCore;

public class ExtensionMetadata {
    public long lastUsed;
    public boolean isActive;

    public ExtensionMetadata(long lastUsed, boolean isActive) {
        this.lastUsed = lastUsed;
        this.isActive = isActive;
    }
}