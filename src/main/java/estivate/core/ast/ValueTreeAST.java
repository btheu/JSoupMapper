package estivate.core.ast;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ValueTreeAST extends ValueAST {

	protected EstivateAST ast;

}
