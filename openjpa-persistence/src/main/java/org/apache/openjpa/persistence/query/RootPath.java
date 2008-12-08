package org.apache.openjpa.persistence.query;

import javax.persistence.DomainObject;

/**
 * Denotes root domain instance representing a persistent type.
 * 
 * @author Pinaki Poddar
 *
 */
public class RootPath extends AbstractDomainObject implements DomainObject {

	public RootPath(QueryDefinitionImpl owner, Class cls) {
		super(owner, null, PathOperator.ROOT, cls);
	}
	
	@Override
	public Class getLastSegment() {
		return (Class)super.getLastSegment();
	}
	
	@Override
	public String getAliasHint(AliasContext ctx) {
		return ctx.getEntityName(getLastSegment());
	}

	@Override
	public String asExpression(AliasContext ctx) {
		return ctx.getAlias(this);
	}
	
	@Override
	public String asJoinable(AliasContext ctx) {
		return ctx.getEntityName(getLastSegment()) + " " + ctx.getAlias(this);
	}
	
	@Override
	public String asProjection(AliasContext ctx) {
		return ctx.getAlias(this);
	}
}
