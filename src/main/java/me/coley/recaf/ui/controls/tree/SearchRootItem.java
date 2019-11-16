package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.search.*;
import me.coley.recaf.util.TypeUtil;
import me.coley.recaf.workspace.JavaResource;

import java.util.*;

/**
 * Root item
 *
 * @author Matt
 */
public class SearchRootItem extends DirectoryItem {
	private final JavaResource resource = resource();

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param results
	 * 		Results to show in sub-items.
	 */
	public SearchRootItem(JavaResource resource, Collection<SearchResult> results) {
		super(resource, null);
		// Add result sub-items in sorted order
		Set<SearchResult> sorted = new TreeSet<>((a, b) -> {
			int cmp = getClassContext(a.getContext()).compareTo(getClassContext(b.getContext()));
			if (cmp == 0)
				return a.compareTo(b);
			else
				return cmp;
		});
		sorted.addAll(results);
		sorted.forEach(this::addResult);
	}

	private void addResult(SearchResult result) {
		// Add the class scope
		Context.ClassContext ctxClass = getClassContext(result.getContext());
		String name = ctxClass.getName();
		DirectoryItem item = this;
		List<String> parts = new ArrayList<>(Arrays.asList(name.split("/")));
		while(!parts.isEmpty()) {
			String part = parts.remove(0);
			boolean isLeaf = parts.isEmpty();
			DirectoryItem child = item.getChild(part, isLeaf);
			if(child == null) {
				child = isLeaf ?
						new ClassItem(resource, part, name) :
						new DirectoryItem(resource, part);
				item.addChild(part, child, isLeaf);
			}
			item = child;
		}
		// Check for if we need to add more scope
		Context<?> ctx = result.getContext();
		if(ctx == ctxClass)
			return;
		if(ctx instanceof Context.MemberContext){
			Context.MemberContext mctx = (Context.MemberContext) ctx;
			item = addMember(item, mctx);
			if (mctx.isField()) {
				if(result instanceof StringResult) {
					String text = ((StringResult) result).getText();
					item.addChild(text, new MiscItem(resource(), text), true);
				} else if(result instanceof ValueResult) {
					String text = ((ValueResult) result).getValue().toString();
					item.addChild(text, new MiscItem(resource(), text), true);
				}
			}
		}
		else if(ctx instanceof Context.InsnContext)
			addInsn(item, (Context.InsnContext) ctx);
		else if(ctx instanceof Context.AnnotationContext) {
			item = addAnno(item, (Context.AnnotationContext) ctx);
			if(result instanceof StringResult) {
				String text = ((StringResult) result).getText();
				item.addChild(text, new MiscItem(resource(), text), true);
			} else if(result instanceof ValueResult) {
				String text = ((ValueResult) result).getValue().toString();
				item.addChild(text, new MiscItem(resource(), text), true);
			}
		}
	}

	private DirectoryItem addMember(DirectoryItem item, Context.MemberContext ctx) {
		// Check existing
		String name = ctx.getName();
		String desc = ctx.getDesc();
		String local = desc.indexOf('(') == 0 ? name + desc : name + " " + desc;
		DirectoryItem mi = item.getChild(local, true);
		if (mi != null)
			return mi;
		// Create new
		mi = new MemberItem(resource(), local, name, desc, ctx.getAccess());
		item.addChild(mi.getLocalName(), mi, true);
		return mi;
	}

	private void addInsn(DirectoryItem item, Context.InsnContext ctx) {
		// Add parent context first
		item = addMember(item, ctx.getParent());
		InsnItem ii = new InsnItem(resource(), ctx.getInsn());
		item.addChild(ii.getLocalName(), ii, true);
	}

	private DirectoryItem addAnno(DirectoryItem item, Context.AnnotationContext ctx) {
		Context<?> ctxParent = ctx.getParent();
		// Check if we must add the parent first
		if(ctxParent instanceof Context.MemberContext)
			item = addMember(item, (Context.MemberContext) ctxParent);
		else if(ctxParent instanceof Context.AnnotationContext)
			item = addAnno(item, (Context.AnnotationContext) ctxParent);
		// Add the annotation
		String name = ctx.getType();
		String local = name.substring(1, name.length() - 1);
		// Check existing
		DirectoryItem ai = item.getChild(local, true);
		if(ai != null)
			return ai;
		// Create new
		ai = new AnnoItem(resource(), local, name);
		item.addChild(local, ai, true);
		return ai;
	}

	private Context.ClassContext getClassContext(Context<?> ctx) {
		while (!ctx.getClass().equals(Context.ClassContext.class))
			ctx = ctx.getParent();
		return (Context.ClassContext) ctx;
	}
}