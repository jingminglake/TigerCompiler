package ast.optimizations;

import java.util.LinkedList;

import ast.Ast.Class;
import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec.DecSingle;
import ast.Ast.Exp;
import ast.Ast.Exp.Add;
import ast.Ast.Exp.And;
import ast.Ast.Exp.ArraySelect;
import ast.Ast.Exp.Call;
import ast.Ast.Exp.False;
import ast.Ast.Exp.Id;
import ast.Ast.Exp.Length;
import ast.Ast.Exp.Lt;
import ast.Ast.Exp.NewIntArray;
import ast.Ast.Exp.NewObject;
import ast.Ast.Exp.Not;
import ast.Ast.Exp.Num;
import ast.Ast.Exp.Sub;
import ast.Ast.Exp.This;
import ast.Ast.Exp.Times;
import ast.Ast.Exp.True;
import ast.Ast.MainClass;
import ast.Ast.MainClass.MainClassSingle;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm;
import ast.Ast.Stm.Assign;
import ast.Ast.Stm.AssignArray;
import ast.Ast.Stm.Block;
import ast.Ast.Stm.If;
import ast.Ast.Stm.Print;
import ast.Ast.Stm.While;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;

// Algebraic simplification optimizations on an AST.

public class AlgSimp implements ast.Visitor {
	public Class.T newClass;
	public MainClass.T mainClass;
	public Program.T program;
	public Exp.T newExp;
	public Stm.T newStm;
	public ast.Ast.Method.T newMethod;
	public boolean isChange;

	public AlgSimp() {
		this.newClass = null;
		this.mainClass = null;
		this.program = null;
		this.newExp = null;
		this.newStm = null;
		this.newMethod = null;
		this.isChange = false;
	}

	// //////////////////////////////////////////////////////
	//
	public String genId() {
		return util.Temp.next();
	}

	// /////////////////////////////////////////////////////
	// expressions
	@Override
	public void visit(Add e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
		if (left instanceof Exp.Num) {
			Exp.Num leftNum = (Exp.Num) left;
			if (leftNum.num == 0) {
				this.isChange = true;
				this.newExp = right;
				return;
			}
		}
		if (right instanceof Exp.Num) {
			Exp.Num rightNum = (Exp.Num) right;
			if (rightNum.num == 0) {
				this.isChange = true;
				this.newExp = left;
				return;
			}
		}
		// 左递归得到最左边的新表达式（简化的表达式）
		e.left.accept(this);
		left = this.newExp;
		// 右递归得到最右边的新表达式（简化的表达式）
		e.right.accept(this);
		right = this.newExp;
		// 返回加号化简后的最终、最新的表达式
		this.newExp = new Exp.Add(left, right);
		return;
	}

	@Override
	public void visit(And e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
		if ((left instanceof Exp.False) || (right instanceof Exp.False)) {
			this.isChange = true;
			this.newExp = new Exp.False();
			return;
		}
		e.left.accept(this);
		left = this.newExp;
		e.right.accept(this);
		right = this.newExp;
		this.newExp = new Exp.And(left, right);
		return;
	}

	@Override
	public void visit(ArraySelect e) {
		e.array.accept(this);
		Exp.T array = this.newExp;
		e.index.accept(this);
		Exp.T index = this.newExp;
		this.newExp = new Exp.ArraySelect(array, index);
	}

	@Override
	public void visit(Call e) {
        this.newExp = e;
		return;
	}

	@Override
	public void visit(False e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Id e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Length e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Lt e) {
		e.left.accept(this);
		Exp.T left = this.newExp;
		e.right.accept(this);
		Exp.T right = this.newExp;
		this.newExp = new Exp.Lt(left, right);
		return;
	}

	@Override
	public void visit(NewIntArray e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(NewObject e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Not e) {
		e.exp.accept(this);
		Exp.T not = this.newExp;
		this.newExp = new Exp.Not(not);
		return;
	}

	@Override
	public void visit(Num e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Sub e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
		if (right instanceof Exp.Num) {
			Exp.Num rightNum = (Exp.Num) right;
			if (rightNum.num == 0) {
				this.isChange = true;
				this.newExp = left;
				return;
			}
		}
		if((left instanceof Exp.Id) && (right instanceof Exp.Id))
		 {
			 Exp.Id leftId=(Exp.Id)left;
			 Exp.Id rightId=(Exp.Id)right;
			 if(leftId.id.equals(rightId.id))
			 {
				 this.isChange = true;
				 this.newExp=new Exp.Num(0);
				 return;
			 }
		 }
		// 左递归得到最左边的新表达式（简化的表达式）
		e.left.accept(this);
		left = this.newExp;
		// 右递归得到最右边的新表达式（简化的表达式）
		e.right.accept(this);
		right = this.newExp;
		// 返回加号化简后的最终、最新的表达式
		this.newExp = new Exp.Sub(left, right);
		return;
	}

	@Override
	public void visit(This e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Times e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
      //  System.out.println("sssssssssssssssss123");
		if (left instanceof Exp.Num) {
			Exp.Num leftNum = (Exp.Num)left;
			if (leftNum.num == 0) {
				this.isChange = true;
				this.newExp = new Exp.Num(0);
				return;
			}
		}
		if (right instanceof Exp.Num) {
			
			Exp.Num rightNum = (Exp.Num)right;
			//System.out.println("sss"+rightNum.num);
			if (rightNum.num == 0) {
			//	System.out.println("sss"+rightNum.num);
				this.isChange = true;
				this.newExp = new Exp.Num(0);
				return;
			}
		}
		e.left.accept(this);
		left = this.newExp;
		e.right.accept(this);
		right = this.newExp;
		this.newExp = new Exp.Times(left, right);
		return;
	}

	@Override
	public void visit(True e) {
		this.newExp = e;
		return;
	}

	// ///////////////////////////////////////
	// statements
	@Override
	public void visit(Assign s) {
        s.exp.accept(this);
        Exp.T a = this.newExp;
        this.newStm = new Stm.Assign(s.id, a);
		return;
	}

	@Override
	public void visit(AssignArray s) {
		s.index.accept(this);
		Exp.T index = this.newExp;
		s.exp.accept(this);
		Exp.T exp = this.newExp;
		this.newStm = new Stm.AssignArray(s.id, index, exp);
		return;
	}

	@Override
	public void visit(Block s) {
		LinkedList<Stm.T> newStms = new LinkedList<Stm.T>();
		for (Stm.T stm : s.stms) {
			stm.accept(this);
			newStms.add(this.newStm);
		}
		this.newStm = new Stm.Block(newStms);
		return;
	}

	@Override
	public void visit(If s) {
        s.condition.accept(this);
        Exp.T condition = this.newExp;
        s.thenn.accept(this);
        Stm.T thenn = this.newStm;
        s.elsee.accept(this);
        Stm.T elsee = this.newStm;
        this.newStm = new Stm.If(condition, thenn, elsee);
		return;
	}

	@Override
	public void visit(Print s) {
		s.exp.accept(this);
		Exp.T exp = this.newExp;
		this.newStm = new Stm.Print(exp);
		return;
	}

	@Override
	public void visit(While s) {
		s.condition.accept(this);
		Exp.T condition = this.newExp;
		s.body.accept(this);
		Stm.T body = this.newStm;
		this.newStm = new Stm.While(condition, body);
		return;
	}

	// type
	@Override
	public void visit(Boolean t) {
	}

	@Override
	public void visit(ClassType t) {
	}

	@Override
	public void visit(Int t) {
	}

	@Override
	public void visit(IntArray t) {
	}

	// dec
	@Override
	public void visit(DecSingle d) {
		return;
	}

	// method
	@Override
	public void visit(MethodSingle m) {
		LinkedList<Stm.T> newStms = new LinkedList<Stm.T>();
		for (Stm.T stm : m.stms) {
			stm.accept(this);
			newStms.add(this.newStm);
		}
		m.retExp.accept(this);
		Exp.T retExp = this.newExp;
		this.newMethod = new ast.Ast.Method.MethodSingle(m.retType, m.id, m.formals, m.locals, newStms, retExp);
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c) {
        LinkedList<ast.Ast.Method.T> newMethods = new LinkedList<ast.Ast.Method.T>();
        for (ast.Ast.Method.T method : c.methods) {
        	method.accept(this);
        	newMethods.add(this.newMethod);
        }
        this.newClass = new ast.Ast.Class.ClassSingle(c.id, c.extendss, c.decs, newMethods);
		return;
	}

	// main class
	@Override
	public void visit(MainClassSingle c) {
        c.stm.accept(this);
        Stm.T stm = this.newStm;
        this.mainClass = new ast.Ast.MainClass.MainClassSingle(c.id, c.arg, stm);
		return;
	}

	// program
	@Override
	public void visit(ProgramSingle p) {

		// You should comment out this line of code:
        p.mainClass.accept(this);
        ast.Ast.MainClass.T mc = this.mainClass;
        LinkedList<ast.Ast.Class.T> newClasses = new LinkedList<ast.Ast.Class.T>();
        for (ast.Ast.Class.T clazz : p.classes) {
        	clazz.accept(this);
        	newClasses.add(this.newClass);
        }
        this.program = new ast.Ast.Program.ProgramSingle(mc, newClasses);
		if (control.Control.trace.contains("ast.AlgSimp")) {
			System.out.println("before optimization:");
			ast.PrettyPrintVisitor pp = new ast.PrettyPrintVisitor();
			p.accept(pp);
			System.out.println("after optimization:");
			this.program.accept(pp);
		}
		return;
	}
}
