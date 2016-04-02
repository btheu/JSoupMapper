package estivate.core.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import estivate.annotations.Attr;
import estivate.annotations.TagName;
import estivate.annotations.Text;
import estivate.annotations.Title;
import estivate.annotations.Val;
import estivate.core.ClassUtils;
import estivate.core.Reducter;
import estivate.core.SelectEvaluater;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultReducter implements Reducter {

    /**
     * Apply all rules (annotations) of type reduce.
     * 
     * @see Attr
     * @see Text
     * @see Title
     * @see TagName
     * @see Val
     * 
     * @param elementSelected
     * @param member
     * @return
     */
    @Override
    public Object reduce(Document document, Elements elementsIn,
            AccessibleObject member) {

        Object value = elementsIn;

        Attr aAttr = member.getAnnotation(Attr.class);
        if (aAttr != null) {

            Elements currElts = SelectEvaluater.select(aAttr, elementsIn,
                    member);

            log.debug("'{}' attr", getName(member));

            log.debug("using attr()", getName(member));

            value = currElts.attr(aAttr.value());

        }

        Val aVal = member.getAnnotation(Val.class);
        if (aVal != null) {

            Elements currElts = SelectEvaluater.select(aVal, elementsIn,
                    member);

            log.debug("'{}' val", getName(member));

            log.debug("using val()", getName(member));

            value = currElts.val();
        }

        TagName aTagName = member.getAnnotation(TagName.class);
        if (aTagName != null) {

            Elements currElts = SelectEvaluater.select(aTagName, elementsIn,
                    member);

            log.debug("'{}' tagName", getName(member));

            log.debug("using tagName()", getName(member));

            value = currElts.first().tagName();
        }

        Title aTitle = member.getAnnotation(Title.class);
        if (aTitle != null) {

            log.debug("'{}' title", getName(member));

            log.debug("using title()", getName(member));

            value = document.title();
        }

        Text aText = member.getAnnotation(Text.class);
        if (aText != null) {
            log.debug("'{}' text", getName(member));

            Elements currElts = SelectEvaluater.select(aText, elementsIn,
                    member);

            if (currElts.size() > 1) {
                log.warn(
                        "'{}' text using first element. Consider fixing the select expression to get only one element.",
                        getName(member));
            }

            log.trace("text in  '{}'", currElts);
            if (aText.own()) {
                log.debug("using first().owntext()");
                value = currElts.first().ownText();
            } else {
                log.debug("using text()");
                value = currElts.text();
            }

            log.trace("text out  '{}'", value);
        }

        return value;
    }

    protected static Object getName(AnnotatedElement member) {
        return ClassUtils.getName(member);
    }

}