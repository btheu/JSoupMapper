package estivate.core.ast;

import java.lang.reflect.Type;

import lombok.Data;

@Data
public class ValueAST {

    @Deprecated
	protected Type type;

    @Deprecated
	protected Class<?> rawClass;

    @Deprecated
	protected boolean isValueList = false;
	
    @Deprecated
	protected Object value;

}
