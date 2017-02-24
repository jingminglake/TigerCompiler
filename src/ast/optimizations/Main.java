package ast.optimizations;

public class Main {
	public ast.Ast.Program.T program;
	public boolean isNotOver; 
	public void accept(ast.Ast.Program.T ast) {
		isNotOver = true;
		int round = 0;
		 while(isNotOver) {
			round++;
			boolean flag_dcz = false;
			boolean flag_dc = false;
			boolean flag_cf = false;
			boolean flag_as= false;
			System.out.println("optimize round : " + round);
			DeadClass dceVisitor = new DeadClass();
			control.CompilerPass deadClassPass = new control.CompilerPass(
					"Dead class elimination", ast, dceVisitor);
			if (control.Control.skipPass("ast.DeadClass")) {
				flag_dcz = false;
			} else {
				deadClassPass.doit();
				ast = dceVisitor.program;
				flag_dcz = dceVisitor.isChange;
			}
			// System.out.println("2222222222222222222222222");

			DeadCode dcodeVisitor = new DeadCode();
			control.CompilerPass deadCodePass = new control.CompilerPass(
					"Dead code elimination", ast, dcodeVisitor);
			if (control.Control.skipPass("ast.DeadCode")) {
				flag_dc = false;
			} else {
				deadCodePass.doit();
				ast = dcodeVisitor.program;
				flag_dc = dcodeVisitor.isChange;
			}
			AlgSimp algVisitor = new AlgSimp();
			control.CompilerPass algPass = new control.CompilerPass(
					"Algebraic simplification", ast, algVisitor);
			if (control.Control.skipPass("ast.AlgSimp")) {
				flag_as = false;
			} else {
				algPass.doit();
				ast = algVisitor.program;
				flag_as = algVisitor.isChange;
			}
			// System.out.println("333333333333333333333333333");
			ConstFold cfVisitor = new ConstFold();
			control.CompilerPass constFoldPass = new control.CompilerPass(
					"Const folding", ast, cfVisitor);
			if (control.Control.skipPass("ast.ConstFold")) {
				flag_cf = false;
			} else {
				constFoldPass.doit();
				ast = cfVisitor.program;
				flag_cf = cfVisitor.isChange;
			}
			isNotOver = flag_dcz || flag_dc || flag_as || flag_cf;
			System.out.println("optimize round " + round + " over");
		}
		System.out.println("\noptimization is as deep as possible,so it's all over");
		program = ast;

		return;
	}
}
