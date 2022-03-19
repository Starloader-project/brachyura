package io.github.coolcrabs.brachyura.util;

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A simple wrapper around {@link NodeList} that implements iterable.
 * It is meant for ease of use inside foreach loops.
 */
public class IterableNodeList implements Iterable<Node> {

    @NotNull
    private final NodeList nodeList;

    public IterableNodeList(NodeList nodeList) {
        if (nodeList == null) {
            throw new NullPointerException("nodeList may not be null!");
        }
        this.nodeList = nodeList;
    }

    @Override
    public Iterator<Node> iterator() {
        return new Iterator<Node>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < nodeList.getLength();
            }

            @Override
            public Node next() {
                if (!hasNext()) {
                    throw new IndexOutOfBoundsException("This iterator is exhausted.");
                }
                return nodeList.item(index++);
            }
        };
    }

    /**
     * Obtains the first element with a specified tag name which is in the list. Not recursive.
     * Should no elements get matched, null is returned.
     *
     * @param tagName The tag name to match
     * @return The resolved element.
     */
    @Nullable
    public Element resolveFirstElement(@NotNull String tagName) {
        for (Node node : this) {
            if (node instanceof Element) {
                Element elem = (Element) node;
                if (tagName.equals(elem.getTagName())) {
                    return elem;
                }
            }
        }
        return null;
    }
}
