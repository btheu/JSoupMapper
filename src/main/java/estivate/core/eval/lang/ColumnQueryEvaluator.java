package estivate.core.eval.lang;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import estivate.core.ast.QueryAST;
import estivate.core.ast.lang.ColumnQueryAST;
import estivate.core.eval.EstivateEvaluator.EvalContext;
import estivate.core.eval.EstivateEvaluator.QueryEvaluator;
import estivate.core.eval.EstivateEvaluatorException;
import estivate.core.eval.lang.TableQueryEvaluator.IntRange;
import estivate.core.eval.lang.TableQueryEvaluator.TableIndex;
import estivate.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ColumnQueryEvaluator implements QueryEvaluator {

    public static final ColumnQueryEvaluator INSTANCE = new ColumnQueryEvaluator();

    public void evalQuery(EvalContext context, QueryAST query) {
        if (query instanceof ColumnQueryAST) {
            ColumnQueryAST ast = (ColumnQueryAST) query;

            Elements currentElements = context.getQueryResult();

            if (log.isDebugEnabled()) {
                currentElements.forEach(new Consumer<Element>() {
                    public void accept(Element t) {
                        log.debug("Column query entry type: {}", t.tag().toString());
                    }
                });
            }

            if (currentElements.size() > 1) {
                throw new EstivateEvaluatorException(context,
                        "Column must be applied on only one tr tag, counted: " + currentElements.size());
            }

            Element row = currentElements.first();
            if (!row.tagName().equalsIgnoreCase("tr")) {
                throw new EstivateEvaluatorException(context,
                        "Column must be applied on tr tag, was: " + row.tagName());
            }

            TableIndex tableIndex = context.getTableIndex();
            if (tableIndex == null) {
                throw new EstivateEvaluatorException(context,
                        "Column annotation must be applied under an @Table annotation");
            }

            context.setQueryResult(new Elements());

            Map<String, IntRange> textColMap = tableIndex.getTextColMap();

            if (ast.getRegex() != null) {
                Pattern regex = ast.getRegex();

                Set<Entry<String, IntRange>> entrySet = textColMap.entrySet();
                for (Entry<String, IntRange> entry : entrySet) {
                    if (regex.matcher(entry.getKey().replaceAll("\\\\/", "/")).matches()) {
                        IntRange indexOf = entry.getValue();
                        context.setQueryResult(TableQueryEvaluator.findTdsInRange(row.select("td"), indexOf));
                        break;
                    }
                }
                if (context.getQueryResult().isEmpty()) {
                    throw new EstivateEvaluatorException(context, "Column not found: " + regex.toString());
                }
            } else if (StringUtil.isNotBlank(ast.getColumnName())) {
                String columnName = ast.getColumnName();
                IntRange indexOf = textColMap.get(columnName);
                if (indexOf == null) {
                    throw new EstivateEvaluatorException(context, "Column not found: " + columnName);
                }

                context.setQueryResult(TableQueryEvaluator.findTdsInRange(row.select("td"), indexOf));
            } else if (StringUtil.isNotBlank(ast.getHavingExpr())) {
                String havingExpr = ast.getHavingExpr();
                IntRange indexOf = context.getTableIndex().eval(havingExpr);
                if (indexOf == null) {
                    throw new EstivateEvaluatorException(context, "Column not found: " + havingExpr);
                }

                context.setQueryResult(TableQueryEvaluator.findTdsInRange(row.select("td"), indexOf));
            }
        }

    }

}
