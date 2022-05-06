package io.github.coolcrabs.brachyura.mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.github.coolcrabs.brachyura.mixin.TinyTinyMappingsReader.TinyTree.TinyClass;
import io.github.coolcrabs.brachyura.mixin.TinyTinyMappingsReader.TinyTree.TinyClass.TinyField;
import io.github.coolcrabs.brachyura.mixin.TinyTinyMappingsReader.TinyTree.TinyClass.TinyMethod;

// Avoid external deps
class TinyTinyMappingsReader {
    private TinyTinyMappingsReader() { }

    static TinyTree read(BufferedReader reader) throws IOException {
        String[] header = reader.readLine().split("\t");
        if (!header[0].equals("tiny") || !header[1].equals("2")) throw new IOException("Invalid header");
        String[] namespaces = new String[header.length - 3];
        System.arraycopy(header, 3, namespaces, 0, namespaces.length);
        TinyTree r = new TinyTree(namespaces);
        String line;
        TinyClass currentClass = null;
        while ((line = reader.readLine()) != null) {
            int indent = 0;
            while (line.charAt(indent) == '\t') {
                indent++;
            }
            // Important: Keep trailing empty strings
            // See: https://stackoverflow.com/a/14602089
            String[] parts = line.substring(indent).split("\t", -1);
            if (indent == 0 && parts[0].equals("c")) {
                currentClass = new TinyClass();
                currentClass.names = new String[r.namespaces.length];
                // SlBrachyura: Quilt mappings are whack
                if (parts.length - 1 < r.namespaces.length) {
                    List<String> strictlySplit = new ArrayList<>();
                    int start = indent;
                    while (true) {
                        int end = line.indexOf('\t', start + 1);
                        if (start != indent) {
                            start++;
                        }
                        if (end == -1) {
                            strictlySplit.add(line.substring(start));
                            break;
                        }
                        strictlySplit.add(line.substring(start, end));
                        start = end;
                    }
                    parts = strictlySplit.toArray(new String[0]);
                    if (parts.length - 1 < r.namespaces.length) {
                        throw new ArrayIndexOutOfBoundsException("Invalid line (parts length is " + parts.length
                                + ", namespaces length is " + r.namespaces.length + "): " + line);
                    }
                }
                System.arraycopy(parts, 1, currentClass.names, 0, r.namespaces.length);
                for (int i = 0; i < r.namespaces.length; i++) {
                    String a = currentClass.names[i];
                    if (!a.isEmpty()) {
                        r.classmaps[i].put(a, currentClass);
                    }
                }
            } else if (indent == 1 && parts[0].equals("m")) {
                TinyMethod method = new TinyMethod();
                method.desc = parts[1];
                method.name = new String[r.namespaces.length];
                System.arraycopy(parts, 2, method.name, 0, r.namespaces.length);
                Objects.requireNonNull(currentClass).methods.add(method);
            } else if (indent == 1 && parts[0].equals("f")) {
                TinyField field = new TinyField();
                field.desc = parts[1];
                field.name = new String[r.namespaces.length];
                System.arraycopy(parts, 2, field.name, 0, r.namespaces.length);
                Objects.requireNonNull(currentClass).fields.add(field);
            }
        }
        return r;
    }

    static class TinyTree {
        String[] namespaces;
        HashMap<String, TinyClass>[] classmaps;

        @SuppressWarnings("unchecked") // I believe in more modern java we can work around this warning
        TinyTree(String[] namespaces) {
            this.namespaces = namespaces;
            classmaps = new HashMap[namespaces.length];
            for (int i = 0; i < classmaps.length; i++) {
                classmaps[i] = new HashMap<>();
            }
        }

        int getNamespace(String name) {
            for (int i = 0; i < namespaces.length; i++) {
                if (namespaces[i].equals(name)) return i;
            }
            throw new RuntimeException("Unable to find namespace: " + name);
        }

        static class TinyClass {
            String[] names;
            ArrayList<TinyMethod> methods = new ArrayList<>();
            ArrayList<TinyField> fields = new ArrayList<>();

            static class TinyMethod {
                String[] name;
                String desc;

                String getDesc(TinyTree tree, int namespace) {
                    return mapDesc(desc, tree, 0, namespace);
                }
            }

            static class TinyField {
                String[] name;
                String desc;

                String getDesc(TinyTree tree, int namespace) {
                    return mapDesc(desc, tree, 0, namespace);
                }
            }
        }

        // https://github.com/FabricMC/mapping-io/blob/c4a09236b5e6ed6f661683a4b875b1c483772b76/src/main/java/net/fabricmc/mappingio/MappingUtil.java#L26
        public static String mapDesc(String desc, TinyTree tinyTree, int src, int dst) {
            int start = 0;
            int end = desc.length();
            StringBuilder ret = null;
            int searchStart = start;
            int clsStart;

            while ((clsStart = desc.indexOf('L', searchStart)) >= 0) {
                int clsEnd = desc.indexOf(';', clsStart + 1);
                if (clsEnd < 0) throw new IllegalArgumentException();

                String cls = desc.substring(clsStart + 1, clsEnd);
                TinyClass tcls = tinyTree.classmaps[src].get(cls);
                String mappedCls = tcls == null ? null : tcls.names[dst];

                if (mappedCls != null) {
                    if (ret == null) ret = new StringBuilder(end - start);

                    ret.append(desc, start, clsStart + 1);
                    ret.append(mappedCls);
                    start = clsEnd;
                }

                searchStart = clsEnd + 1;
            }

            if (ret == null) return desc.substring(start, end);
    
            ret.append(desc, start, end);
    
            return ret.toString();
        }
    }
}
