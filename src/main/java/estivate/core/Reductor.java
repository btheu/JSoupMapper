package estivate.core;

import java.lang.reflect.AccessibleObject;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * <p>
 * Handle annotation of type 'reduce', aka. returns a final result like text()
 * or attr().
 * 
 * <p>
 * Thoses reductors will be called after {@link Selector}
 * 
 * @see Selector
 * 
 * @author Benoit Theunissen
 *
 */
public interface Reductor {

    Object reduce(Document document, Elements elementsCurr,
            AccessibleObject member);

}
