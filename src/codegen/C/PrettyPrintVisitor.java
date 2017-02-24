package codegen.C;

import ast.Ast.Type;
import codegen.C.Ast.Class.ClassSingle;
import codegen.C.Ast.Dec;
import codegen.C.Ast.Dec.DecSingle;
import codegen.C.Ast.Exp;
import codegen.C.Ast.Exp.Add;
import codegen.C.Ast.Exp.And;
import codegen.C.Ast.Exp.ArraySelect;
import codegen.C.Ast.Exp.Call;
import codegen.C.Ast.Exp.Id;
import codegen.C.Ast.Exp.Length;
import codegen.C.Ast.Exp.Lt;
import codegen.C.Ast.Exp.NewIntArray;
import codegen.C.Ast.Exp.NewObject;
import codegen.C.Ast.Exp.Not;
import codegen.C.Ast.Exp.Num;
import codegen.C.Ast.Exp.Sub;
import codegen.C.Ast.Exp.This;
import codegen.C.Ast.Exp.Times;
import codegen.C.Ast.MainMethod.MainMethodSingle;
import codegen.C.Ast.Method;
import codegen.C.Ast.Method.MethodSingle;
import codegen.C.Ast.Program.ProgramSingle;
import codegen.C.Ast.Stm;
import codegen.C.Ast.Stm.Assign;
import codegen.C.Ast.Stm.AssignArray;
import codegen.C.Ast.Stm.Block;
import codegen.C.Ast.Stm.If;
import codegen.C.Ast.Stm.Print;
import codegen.C.Ast.Stm.While;
import codegen.C.Ast.Type.ClassType;
import codegen.C.Ast.Type.Int;
import codegen.C.Ast.Type.IntArray;
import codegen.C.Ast.Vtable;
import codegen.C.Ast.Vtable.VtableSingle;
import control.Control;

public class PrettyPrintVisitor implements Visitor {
	private int indentLevel;
	private java.io.BufferedWriter writer;
	public java.util.LinkedList<Character> f_arguments_gc_map;
	public java.util.LinkedList<Character> f_locals_gc_map;
    public java.util.LinkedList<String> f_arguments_id;//记录当前方法的参数id，以便后面使用frame.的时候区分
    public java.util.LinkedList<String> f_locals_reference_id;//记录当前方法局部变量是引用的id,以便后面使用frame.的时候区分
    public ProgramSingle program_tmp;
    public PrettyPrintVisitor() {
		this.indentLevel = 2;
	}

	private void indent() {
		this.indentLevel += 2;
	}

	private void unIndent() {
		this.indentLevel -= 2;
	}

	private void printSpaces() {
		int i = this.indentLevel;
		while (i-- != 0)
			this.say(" ");
	}

	private void sayln(String s) {
		say(s);
		try {
			this.writer.write("\n");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void say(String s) {
		try {
			this.writer.write(s);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// /////////////////////////////////////////////////////
	// expressions
	@Override
	public void visit(Add e) {
		e.left.accept(this);
		this.say(" + ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(And e) {
		e.left.accept(this);
		this.say(" && ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(ArraySelect e) {
		e.array.accept(this);
		this.say("[");
		e.index.accept(this);
		this.say("] ");
		return;
	}

	@Override
	public void visit(Call e) {		
		this.say("(");
		if (this.f_arguments_id.contains(e.assign) || !this.f_locals_reference_id.contains(e.assign)) {
			this.say(e.assign + "=");
		}
		else this.say("frame." + e.assign + "=");
		e.exp.accept(this);
		this.say(", ");
		if (this.f_arguments_id.contains(e.assign) || !this.f_locals_reference_id.contains(e.assign)) {
			this.say(e.assign + "->vptr->" + e.id + "(" + e.assign);
		}
		else this.say("frame." + e.assign + "->vptr->" + e.id + "(" + "frame." + e.assign);
		int size = e.args.size();
		if (size == 0) {
			this.say("))");
			return;
		}
		for (Exp.T x : e.args) {
			this.say(", ");
			x.accept(this);
		}
		this.say("))");
		return;
	}

	@Override
	public void visit(Id e) {
	/*	boolean flag = false;
		for(String idd : this.f_arguments_id) {
			if (idd.equals(e.id)) {
				flag = true;
				break;
			}
		}*/
		if(e.isField){
			this.say("this->" + e.id);
		}
		else if(this.f_arguments_id.contains(e.id) || !this.f_locals_reference_id.contains(e.id)) {
			this.say(e.id);
		}
		else this.say("frame." + e.id);
		return;
	}

	@Override
	public void visit(Length e) {
		this.say("*(");
		e.array.accept(this);
		this.say("-1)");
		return;
	}

	@Override
	public void visit(Lt e) {
		e.left.accept(this);
		this.say(" < ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(NewIntArray e) {
		this.say("(int*) Tiger_new_array(");
		e.exp.accept(this);
		this.say(")");
		return;
	}

	@Override
	public void visit(NewObject e) {
		this.say("((struct " + e.id + "*)(Tiger_new (&" + e.id
				+ "_vtable_,sizeof(struct " + e.id + "))))");
		return;
	}

	@Override
	public void visit(Not e) {
		this.say("!(");
		e.exp.accept(this);
		this.say(")");
		return;
	}

	@Override
	public void visit(Num e) {
		this.say(Integer.toString(e.num));
		return;
	}

	@Override
	public void visit(Sub e) {
		e.left.accept(this);
		this.say(" - ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(This e) {
		this.say("this");
	}

	@Override
	public void visit(Times e) {
		e.left.accept(this);
		this.say(" * ");
		e.right.accept(this);
		return;
	}

	// statements
	@Override
	public void visit(Assign s) {
		this.printSpaces();
		if(s.isField) {
			this.say("this->" + s.id + " = ");
		}
		else if(this.f_arguments_id.contains(s.id) || !this.f_locals_reference_id.contains(s.id)) {
			this.say(s.id + " = ");
		}
		else this.say("frame." + s.id + " = ");
		s.exp.accept(this);
		this.sayln(";");
		return;
	}

	@Override
	public void visit(AssignArray s) {
		this.printSpaces();
		if (s.isField) {
			this.say("this->" + s.id + "[");
		}
		else if(this.f_arguments_id.contains(s.id) || !this.f_locals_reference_id.contains(s.id)) {
			this.say(s.id + "[");
		}
		else this.say("frame." + s.id + "[");
		s.index.accept(this);
		this.say("] = ");
		s.exp.accept(this);
		this.sayln(";");
	}

	@Override
	public void visit(Block s) {
		for (Stm.T s1 : s.stms)
			s1.accept(this);
	}

	@Override
	public void visit(If s) {
		this.printSpaces();
		this.say("if (");
		s.condition.accept(this);
		this.sayln(")");
		if (s.thenn instanceof Stm.Block) {
			this.printSpaces();
			this.sayln("{");
		}
		this.indent();
		s.thenn.accept(this);
		this.unIndent();
		if (s.thenn instanceof Stm.Block) {
			this.printSpaces();
			this.sayln("}");
		}
		else this.sayln("");
		this.printSpaces();
		this.sayln("else");
		if (s.elsee instanceof Stm.Block) {
			this.printSpaces();
			this.sayln("{");
		}
		this.indent();
		s.elsee.accept(this);
		this.unIndent();
		if (s.elsee instanceof Stm.Block) {
			this.printSpaces();
			this.sayln("}");
		}
		else this.sayln("");
		return;
	}

	@Override
	public void visit(Print s) {
		this.printSpaces();
		this.say("System_out_println (");
		s.exp.accept(this);
		this.sayln(");");
		return;
	}

	@Override
	public void visit(While s) {
		this.printSpaces();
		this.say("while (");
		s.condition.accept(this);
		this.sayln(")");
		this.printSpaces();
		this.sayln("{");
		this.indent();
		s.body.accept(this);
		this.unIndent();
		this.printSpaces();
		this.sayln("}");
	}

	// type
	@Override
	public void visit(ClassType t) {
		// System.out.println("1111111111111  "+t.id);
		this.say("struct " + t.id + " *");
	}

	@Override
	public void visit(Int t) {
		this.say("int");
	}

	@Override
	public void visit(IntArray t) {
		this.say("int*");
	}

	// dec
	@Override
	public void visit(DecSingle d) {
		d.type.accept(this);
		this.say(" " + d.id + ";");

	}

	// method
	@Override
	public void visit(MethodSingle m) {
		//初始化每个方法的记录信息的各种表
	    f_arguments_gc_map = new java.util.LinkedList<Character>();
	    f_locals_gc_map = new java.util.LinkedList<Character>();
	    f_arguments_id = new java.util.LinkedList<String>();
	    f_locals_reference_id = new java.util.LinkedList<String>();
	    //方法之前打印该方法栈帧结构体
	    this.sayln("struct " + m.classId + "_"+ m.id + "_gc_frame");
	    this.sayln("{");
	    this.printSpaces();
	    this.sayln("void *prev;");
	    this.printSpaces();
	    this.sayln("char *arguments_gc_map;");
	    this.printSpaces();
	    this.sayln("int *arguments_base_address;");
	    this.printSpaces();
	    this.sayln("char *locals_gc_map;");
	    for (Dec.T d : m.locals) {
			DecSingle dec = (DecSingle) d;
			if (dec.type instanceof codegen.C.Ast.Type.ClassType) {
				this.printSpaces();
				dec.type.accept(this);		
				this.sayln(" " +  dec.id + ";");
				f_locals_reference_id.addLast(dec.id);//记录方法的局部变量中的引用变量的id
			}
	    }
	    this.sayln("};");//结构体结束
	    
	    //方法之前打印方法参数和局部变量gc图
		int size_formals = m.formals.size();
	    int size_locals = m.locals.size();
	    int size = size_formals;
	    for (Dec.T d : m.formals) {
			DecSingle dec = (DecSingle) d;
			size--;
			if (dec.type instanceof codegen.C.Ast.Type.Int) {
				f_arguments_gc_map.addLast('0');
			}
			else f_arguments_gc_map.addLast('1');
			//在打印语句之前，记录下该方法的参数id
			f_arguments_id.addLast(dec.id);
		}
	    for (Dec.T d : m.locals) {
			DecSingle dec = (DecSingle) d;
			if (dec.type instanceof codegen.C.Ast.Type.Int) {
				f_locals_gc_map.addLast('0');
			}
			else f_locals_gc_map.addLast('1');
		}
	  	this.sayln("// one GC map for method arguments and another one for method locals");
	  	this.say("char *");
		this.say(m.classId + "_" + m.id + "_arguments_gc_map = \"");
		for (int i = 0; i < size_formals; i++) {
	      	this.say(String.valueOf(f_arguments_gc_map.get(i)));
	    }
		this.sayln("\";");	  		
	  	this.say("char *");
	  	this.say(m.classId + "_" + m.id + "_locals_gc_map = \"");
		for (int i = 0; i < size_locals; i++) {
			this.say(String.valueOf(f_locals_gc_map.get(i)));
	  	}
	  	this.sayln("\";");	    
	    //打印方法头
		m.retType.accept(this);
		this.say(" " + m.classId + "_" + m.id + "(");
		size = size_formals;
		//打印方法参数
		for (Dec.T d : m.formals) {
			DecSingle dec = (DecSingle) d;
			size--;
			dec.type.accept(this);
			this.say(" " + dec.id);
			if (size > 0)
				this.say(", ");
		}
		this.sayln(")");
		this.sayln("{");

		//打印方法gc栈帧赋值信息 //////////////////
		this.printSpaces();
		this.sayln("struct " + m.classId + "_" + m.id + "_gc_frame frame;");
		this.printSpaces();
		this.sayln("frame.prev = prev;");
		this.printSpaces();
		this.sayln("prev = &frame;");
		this.printSpaces();
		this.sayln("frame.arguments_gc_map = " + m.classId + "_" + m.id + "_arguments_gc_map;");
		this.printSpaces();
		this.sayln("frame.arguments_base_address = &this;");
		this.printSpaces();
		this.sayln("frame.locals_gc_map = " + m.classId + "_" + m.id + "_locals_gc_map;");
		//打印方法的原来的局部变量
		for (Dec.T d : m.locals) {
			DecSingle dec = (DecSingle) d;
			this.say("  ");
			dec.type.accept(this);
			// System.out.println(dec.id);
			this.say(" " + dec.id + ";\n");
		}
		//为栈帧中的引用变量属性赋值
		for (Dec.T d : m.locals) {
			DecSingle dec = (DecSingle) d;
			if (dec.type instanceof codegen.C.Ast.Type.ClassType) {
				this.printSpaces();
				this.sayln("frame." + dec.id + " = " + dec.id + ";");
			}
		} 
		//打印方法的语句部分 ////////////////////
		this.sayln("");
		for (Stm.T s : m.stms)
			s.accept(this);
		//return之前弹frame栈
	/*	this.printSpaces();
		this.sayln("prev = frame.prev;");*/
		this.say("  return ");
		m.retExp.accept(this);
		this.sayln(";");
		this.sayln("}");
		return; 
	}

	@Override
	public void visit(MainMethodSingle m) {
		//初始化每个方法的记录信息的各种表
	    f_arguments_gc_map = new java.util.LinkedList<Character>();
	    f_locals_gc_map = new java.util.LinkedList<Character>();
	    f_arguments_id = new java.util.LinkedList<String>();
	    f_locals_reference_id = new java.util.LinkedList<String>();
	    //方法之前打印该方法栈帧结构体
	    this.sayln("struct Tiger_main_gc_frame");
	    this.sayln("{");
	    this.printSpaces();
	    this.sayln("void *prev;");
	    this.printSpaces();
	    this.sayln("char *arguments_gc_map;");
	    this.printSpaces();
	    this.sayln("int *arguments_base_address;");
	    this.printSpaces();
	    this.sayln("char *locals_gc_map;");
	    for (Dec.T d : m.locals) {
			DecSingle dec = (DecSingle) d;
			if (dec.type instanceof codegen.C.Ast.Type.ClassType) {
				this.printSpaces();
				dec.type.accept(this);		
				this.sayln(" " +  dec.id + ";");
				f_locals_reference_id.addLast(dec.id);//记录方法的局部变量中的引用变量的id
			}
	    }
	    this.sayln("};");//结构体结束
	    
	  //方法之前打印方法参数和局部变量gc图
	    int size_locals = m.locals.size();
	    for (Dec.T d : m.locals) {
  			DecSingle dec = (DecSingle) d;
  			if (dec.type instanceof codegen.C.Ast.Type.Int) {
	  			f_locals_gc_map.addLast('0');
	  		}
			else f_locals_gc_map.addLast('1');
	  		}
	  	this.sayln("// one GC map for method arguments and another one for method locals");
	    this.say("char *");
	  	this.sayln("Tiger_main_arguments_gc_map = \"\";");
		
	  	this.say("char *");
	  	this.say("Tiger_main_locals_gc_map = \"");
	  	for (int i = 0; i < size_locals; i++) {
	  		this.say(String.valueOf(f_locals_gc_map.get(i)));
	  	}
	  	this.sayln("\";");	    
	    //打印方法头
		this.sayln("int Tiger_main ()");
		this.sayln("{");
		//打印方法gc栈帧赋值信息 //////////////////
		this.printSpaces();
		this.sayln("struct Tiger_main_gc_frame frame;");
		this.printSpaces();
		this.sayln("frame.prev = prev;");
		this.printSpaces();
		this.sayln("prev = &frame;");
		this.printSpaces();
		this.sayln("frame.arguments_gc_map = Tiger_main_arguments_gc_map;");
		this.printSpaces();
		this.sayln("frame.arguments_base_address = 0;");
		this.printSpaces();
		this.sayln("frame.locals_gc_map = Tiger_main_locals_gc_map;");
		//打印原来的局部变量(对main方法来说，就是为new赋的id)
		for (Dec.T dec : m.locals) {
			this.say("  ");
			DecSingle d = (DecSingle) dec;
			// System.out.println(d.type);
			d.type.accept(this);
			this.say(" ");
			this.sayln(d.id + ";");
		}
		//为栈帧的引用变量属性域赋值
		for (Dec.T d : m.locals) {
			DecSingle dec = (DecSingle) d;
			if (dec.type instanceof codegen.C.Ast.Type.ClassType) {
				this.printSpaces();
				this.sayln("frame." + dec.id + " = " + dec.id + ";");
			}
		}
		//打印方法的语句部分 ////////////////////
		m.stm.accept(this);
		//方法结束之前弹frame栈
	/*	this.printSpaces();
		this.sayln("prev = frame.prev;");*/
		this.sayln("}\n");
		return;
	}

	// vtables
	@Override
	public void visit(VtableSingle v) {
		this.sayln("struct " + v.id + "_vtable");
		this.sayln("{");
		this.printSpaces();
		this.sayln("char *" + v.id + "_gc_map;");
		for (codegen.C.Ftuple t : v.ms) {
			this.say("  ");
			t.ret.accept(this);
			this.sayln(" (*" + t.id + ")();");
		}
		this.sayln("};\n");
		return;
	}

	private void outputVtable(VtableSingle v) {
		this.sayln("struct " + v.id + "_vtable " + v.id + "_vtable_ = ");
		this.sayln("{");
		//打印类变量的class_locals_gc_map de zhi
		this.printSpaces();
        for (codegen.C.Ast.Class.T c : this.program_tmp.classes) {
        	codegen.C.Ast.Class.ClassSingle cc = (codegen.C.Ast.Class.ClassSingle) c;
        	//收集类的变量信息并打印
        	if (cc.id.equals(v.id)) {
            	this.say("\"");
				for (codegen.C.Tuple t : cc.decs) {
					 if (t.type instanceof codegen.C.Ast.Type.Int) {
						 this.say("0");
					 }
					 else this.say("1");
				}
				this.sayln("\",");
        	}
		}
		
		
		
		//this.sayln("&" + v.id + "_gc_map,");
		
		for (codegen.C.Ftuple t : v.ms) {
			this.say("  ");
			this.sayln(t.classs + "_" + t.id + ",");
		}
		this.sayln("};\n");
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c) {
		this.sayln("struct " + c.id);
		this.sayln("{");
		this.sayln("  struct " + c.id + "_vtable *vptr;");
		//新增gc功能的对象模型
		this.sayln("  int isObjOrArray;");//是普通类还是整型对象数组
		this.sayln("  unsigned length;");//对象数组的长度
		this.sayln("  void *forwarding;");
		// 打印类的类变量声明
		for (codegen.C.Tuple t : c.decs) {
			this.say("  ");
			t.type.accept(this);
			this.say(" ");
			this.sayln(t.id + ";");
		}
		this.sayln("};");
		return;
	}

	// program
	@Override
	public void visit(ProgramSingle p) {
		this.program_tmp = p;
		// we'd like to output to a file, rather than the "stdout".
		try {
			String outputName = null;
			if (Control.ConCodeGen.outputName != null)
				outputName = Control.ConCodeGen.outputName;
			else if (Control.ConCodeGen.fileName != null)
				outputName = Control.ConCodeGen.fileName + ".c";
			else
				outputName = "a.c";

			this.writer = new java.io.BufferedWriter(
					new java.io.OutputStreamWriter(
							new java.io.FileOutputStream(outputName)));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		this.sayln("// This is automatically generated by the Tiger compiler.");
		this.sayln("// Do NOT modify!\n");
		
		this.sayln("\n// a global pointer");
        this.sayln("void *prev = 0;");
        //打印全部类变量的class_locals_gc_map
        this.sayln("// class_locals_gc_map");
        for (codegen.C.Ast.Class.T c : p.classes) {
        	codegen.C.Ast.Class.ClassSingle cc = (codegen.C.Ast.Class.ClassSingle) c;
        	//收集类的变量信息并打印
        	this.say("char *" + cc.id + "_gc_map = \"");
			for (codegen.C.Tuple t : cc.decs) {
				 if (t.type instanceof codegen.C.Ast.Type.Int) {
					 this.say("0");
				 }
				 else this.say("1");
			}
			this.sayln("\";");
		}
		this.sayln("// structures");
		for (codegen.C.Ast.Class.T c : p.classes) {
			c.accept(this);//打印结构体
		}
		this.sayln("");
		
		this.sayln("// vtables structures");
		for (Vtable.T v : p.vtables) {
			v.accept(this);
		}
		this.sayln("");
		
		this.sayln("// methods dec");
	    for (Method.T m : p.methods) {
	    	MethodSingle ms = (MethodSingle)m;
	    	ms.retType.accept(this);
	        this.say(" " + ms.classId + "_" + ms.id + "(");
	        int size = ms.formals.size();
	        for (Dec.T d : ms.formals) {
	          DecSingle dec = (DecSingle) d;
	          size--;
	          dec.type.accept(this);
	          this.say(" " + dec.id);
	          if (size > 0)
	            this.say(", ");
	        }
	        this.sayln(");");
	    }
		this.sayln("");

		this.sayln("// vtables");
		for (Vtable.T v : p.vtables) {
			outputVtable((VtableSingle) v);
		}
		this.sayln("");
		
		this.sayln("// methods");
		for (Method.T m : p.methods) {
			//MethodSingle ms = (MethodSingle)m;
			m.accept(this);
		}
		this.sayln("");

		this.sayln("// main method");
		p.mainMethod.accept(this);
		this.sayln("");

		this.say("\n\n");

		try {
			this.writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

}
