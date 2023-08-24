package soot.jimple.infoflow.android.plugin.wear.analysis;

public class PairResult<Unit, Expr> {
	/**
	 * The first element of this <code>Pair</code>
	 */
	private Unit unit;

	/**
	 * The second element of this <code>Pair</code>
	 */
	private Expr expr;

	/**
	 * Constructs a new <code>Pair</code> with the given values.
	 * 
	 * @param first
	 *            the first element
	 * @param second
	 *            the second element
	 * @return
	 */
	public PairResult(Unit first, Expr second) {

		this.setUnit(first);
		this.setExpr(second);
	}

	public PairResult() {
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public Expr getExpr() {
		return expr;
	}

	public void setExpr(Expr expr) {
		this.expr = expr;
	}

	@Override
	public String toString() {
		return this.unit.toString();
	}

}
