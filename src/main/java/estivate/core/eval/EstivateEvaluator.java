package estivate.core.eval;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import estivate.core.ClassUtils;
import estivate.core.ast.EstivateAST;
import estivate.core.ast.ExpressionAST;
import estivate.core.ast.ExpressionsAST;
import estivate.core.ast.FieldExpressionAST;
import estivate.core.ast.MethodExpressionAST;
import estivate.core.ast.QueryAST;
import estivate.core.ast.ReduceAST;
import estivate.core.ast.ValueAST;
import estivate.core.eval.ReduceASTEvaluator.ReduceResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EstivateEvaluator {

    public static final List<QueryASTEvaluator.Factory> queryEvalFacts = new ArrayList<QueryASTEvaluator.Factory>();
    static {
        queryEvalFacts.add(EmptyQueryEvaluator.factory);
        queryEvalFacts.add(SelectQueryEvaluator.factory);
    }
    public static final List<ReduceASTEvaluator.Factory> reduceEvalFacts = new ArrayList<ReduceASTEvaluator.Factory>();
    static {
    	reduceEvalFacts.add(EmptyReduceEvaluator.factory);
        reduceEvalFacts.add(TextReduceEvaluator.factory);
        reduceEvalFacts.add(AttrReduceEvaluator.factory);
    }
    public static final List<ExpressionASTEvaluator.Factory> expressionEvalFacts = new ArrayList<ExpressionASTEvaluator.Factory>();
    static {
        expressionEvalFacts.add(FieldExpEvaluater.factory);
        expressionEvalFacts.add(MethodExpEvaluater.factory);
    }

    
    public static Object eval(Document document, EstivateAST ast) {

    	Object target = ClassUtils.newInstance(ast.getTargetRawClass());
        
        EvalContext context = new EvalContext.EvalContextBuilder()
                .document(document)
                .dom(new Elements(document))
                .target(target)
                .optional(ast.isOptional())
                .build();

        evalExpressions(context, ast);

        return target;
    }

    public static void evalExpressions(EvalContext context, ExpressionsAST ast) {
        List<ExpressionAST> expressions = ast.getExpressions();
        for (ExpressionAST expression : expressions) {
            // copy of the context
            evalExpression(context.toBuilder().build(), expression);
        }
    }

    protected static void evalExpression(EvalContext context, ExpressionAST expression) {
        boolean noFound = true;
        for (ExpressionASTEvaluator.Factory evalFact : expressionEvalFacts) {
            ExpressionASTEvaluator eval = evalFact.expressionEvaluater(expression);
            if(eval != null){
                eval.eval(context.toBuilder().build(), expression);
                noFound = false;
            }
        }
        if(noFound){
            log.error("No Expression Evaluater found for '{}'",expression.getClass().getSimpleName());
        }
    }

    protected static EvalContext evalQuery(EvalContext context, QueryAST query) {
        for (QueryASTEvaluator.Factory factory : queryEvalFacts) {
            QueryASTEvaluator eval = factory.expressionEvaluater(query);
            if(eval != null){
                
                log.debug("> Eval '{}' with dom '{}'",
                        query.getClass().getSimpleName(),context.getDom());
                
                EvalContext result = eval.eval(context, query);
                
                log.debug("< Eval '{}' got result '{}'",
                        query.getClass().getSimpleName(),result.getDom());
                
                return result;
            }
        }
        log.error("No Query Evaluater found for '{}'",query.getClass().getSimpleName());
        return null;
    }

    protected static ReduceResult evalReduce(EvalContext context, ReduceAST reduce, boolean isValueList) {
        for (ReduceASTEvaluator.Factory factory : reduceEvalFacts) {
            ReduceASTEvaluator eval = factory.expressionEvaluater(reduce);
            if(eval != null){
                log.debug("> Eval '{}' with dom '{}'",
                        reduce.getClass().getSimpleName(),context.getDom());
                
                ReduceResult result = eval.eval(context, reduce, isValueList);
                
                log.debug("< Eval '{}' got result '{}'",
                        reduce.getClass().getSimpleName(),result.getValue());
                
                return result;
            }
        }
        log.error("No Reduce Evaluater found for '{}'",reduce.getClass().getSimpleName());
        return ReduceResult.builder().build();
    }

	protected static void evalValue(EvalContext context, ReduceResult reduceResult, ValueAST value) {

		if (value.getRawClass().equals(Document.class)) {
			value.setValue(context.getDocument());
		} else if (value.getRawClass().equals(Elements.class)) {
			value.setValue(context.getDom());
		} else if (value.getRawClass().equals(Element.class)) {
			Elements dom = context.getDom();
			if (dom.size() == 1) {
				value.setValue(dom);
			} else {
				throw new IllegalArgumentException("Cant eval single Element value. Size of the DOM was '" + dom.size() + "'");
			}
		} else {
			value.setValue(reduceResult.getValue());
		}

		// TODO Convert
	}
	
	protected static void setValue(EvalContext context, FieldExpressionAST exp) {
		ClassUtils.setValue(exp.getField(), context.getTarget(), exp.getValue().getValue());
	}
	
	protected static void setValues(EvalContext context, MethodExpressionAST exp) {
		Object[] arguments = new Object[exp.getArguments().size()];
		
		for (int i = 0; i < exp.getArguments().size(); i++) {
			arguments[i] = exp.getArguments().get(i).getValue();
		}
		if(arguments.length == 0){
			ClassUtils.setValue(exp.getMethod(), context.getTarget());
		}else{
			ClassUtils.setValue(exp.getMethod(), context.getTarget(), arguments);
		}
	}
	
    public static class FieldExpEvaluater implements ExpressionASTEvaluator{

		public void eval(EvalContext context, ExpressionAST expression) {
			FieldExpressionAST exp = (FieldExpressionAST) expression;
			log.trace("> eval field '{}'", exp.getField().getName());

			EvalContext contextSelect = evalQuery(context.toBuilder().build(), exp.getQuery());

			ReduceResult reduce = evalReduce(contextSelect.toBuilder().build(), exp.getReduce(), exp.getValue().isValueList());

			evalValue(contextSelect, reduce, exp.getValue());

			setValue(contextSelect, exp);

			log.trace("< eval field");
		}

		public static ExpressionASTEvaluator.Factory factory = new ExpressionASTEvaluator.Factory() {

            @Override
            public ExpressionASTEvaluator expressionEvaluater(ExpressionAST expression) {
                if(expression instanceof FieldExpressionAST){
                    return new FieldExpEvaluater();
                }
                return super.expressionEvaluater(expression);
            }
        };
    }

    public static class MethodExpEvaluater implements ExpressionASTEvaluator{

		public void eval(EvalContext context, ExpressionAST expression) {
			MethodExpressionAST exp = (MethodExpressionAST) expression;
			log.trace("> eval method '{}()'", exp.getMethod().getName());

			EvalContext contextSelect = evalQuery(context.toBuilder().build(), exp.getQuery());

			List<ValueAST> arguments = exp.getArguments();
			for (ValueAST valueAST : arguments) {
				ReduceResult reduce = evalReduce(contextSelect.toBuilder().build(), exp.getReduce(), valueAST.isValueList());
				
				evalValue(contextSelect, reduce, valueAST);
			}

			setValues(contextSelect, exp);

			log.trace("< eval method");
		}

        public static Factory factory = new ExpressionASTEvaluator.Factory() {

            @Override
            public ExpressionASTEvaluator expressionEvaluater(ExpressionAST expression) {
                if(expression instanceof MethodExpressionAST){
                    return new MethodExpEvaluater();
                }
                return super.expressionEvaluater(expression);
            }
        };
    }

    @Data
    @Builder(toBuilder=true)
    public static class EvalContext {

        protected boolean optional = false;

        protected Document document;

        protected Elements dom;

        protected Object target;
        
    }

}