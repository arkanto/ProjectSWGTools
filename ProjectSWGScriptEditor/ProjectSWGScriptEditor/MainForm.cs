using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.IO;

namespace ProjectSWGScriptEditor
{
	public partial class MainForm : Form
	{
		static String scriptDirectory = "./";

		public MainForm()
		{
			if (!Config.containsKey("scriptsDirectory")) Config.setValue("scriptsDirectory", scriptDirectory);
			else scriptDirectory = Config.returnValue("scriptsDirectory");

			InitializeComponent();
			loadContentToTreeView();
		}

		private void loadContentToTreeView()
		{
			string[] directories = Directory.GetDirectories(scriptDirectory);
			string[] files = Directory.GetFiles(scriptDirectory);

			for (int i = 0; i < directories.Length; i++) treeView1.Nodes.Add(directories[i].Replace(scriptDirectory, ""));
			for (int i = 0; i < files.Length; i++) treeView1.Nodes.Add(files[i].Replace(scriptDirectory, ""));
		}

		private void setScriptPathToolStripMenuItem_Click(object sender, EventArgs e)
		{
			using (FolderBrowserDialog dialog = new FolderBrowserDialog())
			{
				if (dialog.ShowDialog() == DialogResult.OK) scriptDirectory = dialog.SelectedPath;
			}
			Config.setValue("scriptsDirectory", scriptDirectory);

			treeView1.Nodes.Clear();
			loadContentToTreeView();
		}

		private void treeView1_AfterSelect(object sender, TreeViewEventArgs e)
		{
			string directory = scriptDirectory + getNodeFilePath(treeView1.SelectedNode);
			string childDirectory = getNodeFilePath(treeView1.SelectedNode);

			Console.WriteLine(childDirectory);

			if (!treeView1.SelectedNode.Text.EndsWith(".py"))
			{
				string[] directories = Directory.GetDirectories(directory);
				string[] files = Directory.GetFiles(directory);

				for (int i = 0; i < directories.Length; i++) treeView1.SelectedNode.Nodes.Add(directories[i].Replace(directory, ""));
				for (int i = 0; i < files.Length; i++) treeView1.SelectedNode.Nodes.Add(files[i].Replace(directory, ""));
			}
			else if (treeView1.SelectedNode.Text.EndsWith(".py"))
			{
				scriptViewerTb.Text = File.ReadAllText(directory);

				if (childDirectory.StartsWith(@"\equipment\bonus_sets\"))
				{
					editorNameLbl.Text = "Bonus Set Editor";

					Editors.BonusSetEditor editor = new Editors.BonusSetEditor(directory);
				}
				else if (childDirectory == "")
				{

				}
				else editorNameLbl.Text = "Unknown script type";
			}
		}

		private string getNodeFilePath(TreeNode node)
		{
			if(node.Parent == null) return node.Text;
			return getNodeFilePath(node.Parent) + node.Text;
		}
	}
}
