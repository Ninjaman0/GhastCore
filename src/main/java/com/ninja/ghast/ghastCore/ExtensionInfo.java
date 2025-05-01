package com.ninja.ghast.ghastCore;

public class ExtensionInfo {
    public final String name;
    public final String author;
    public final String version;
    public final String namespace;

    public ExtensionInfo(String name, String author, String version, String namespace) {
        this.name = name;
        this.author = author;
        this.version = version;
        this.namespace = namespace;
    }
}