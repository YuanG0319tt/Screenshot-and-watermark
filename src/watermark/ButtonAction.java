package watermark;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileNameExtensionFilter;

import watermark.CommandButton.ButtonStyle;

public class ButtonAction implements ActionListener {
	private ButtonStyle style;
	private MainFrame mainFrame;
	public static boolean isSingle = true;
    private static int completeCount = 0;//进度条的线程
	private JFileChooser fileChooser = new JFileChooser();//文件选择对话框
	public static ArrayList<FileBean> array;//批量的图片
	public static boolean isDrag = false;//是拖拉的
	public ButtonAction(ButtonStyle style){
		this.style = style;
	}
	public void actionPerformed(ActionEvent e) {

		fileChooser.updateUI();// 将 UI 属性重置为当前的外观值。
		mainFrame = MainFrame.getInstance();
		if (style.equals(ButtonStyle.batching)) {//批量添加监听
			if (isSingle) {
				mainFrame.setSelectPathBtText("选择文件夹");
				mainFrame.setBatchingBtText("单个文件");
				mainFrame.setTitle("添加文字-当前状态：批量添加");
				isSingle = false;
			} else {
				mainFrame.setSelectPathBtText("选择图片");
				mainFrame.setBatchingBtText("批量添加");
				mainFrame.setTitle("添加水印-当前状态：单个文件");
				isSingle = true;
			}
		} else if (style.equals(ButtonStyle.selectImage)) {//选择文件监听
			String result = getSelectResult();
			if (result != null) {
				mainFrame.getFilepathTF().setText(result);
				mainFrame.getSavepathTF().setText(
						result.substring(0, result.lastIndexOf("\\")) + Common.getNewFileorDirName(result));
			}
		} else if (style.equals(ButtonStyle.selectSavepath)) {//存放文件路径监听
			String result = getSelectResult();
			if (result != null) {
				mainFrame.getSavepathTF().setText(
						result + Common.getNewFileorDirName(mainFrame.getFilepath()));
			}
		} else if (style.equals(ButtonStyle.preview)) {//预览监听
			String firstFilepath = getFirstFilepath();
			if(firstFilepath ==  null){
				JOptionPane.showMessageDialog(null, "找不到图片文件！");//弹框提示
				return;
			}
			BufferedImage buffImg = addWatermark(firstFilepath);
			new PreviewImage(buffImg);
		} else if (style.equals(ButtonStyle.drirect)) {//添加文字监听
			String savePath = mainFrame.getSavePath();
			if (savePath.equals("")) {
				JOptionPane.showMessageDialog(null, "请选择存放路径！");//弹框提示
				return;
			}
			if (isSingle) {
				// 对图像加水印
				BufferedImage buffImg = addWatermark(mainFrame.getFilepath());
				// 生成水印图片
				generate(buffImg, savePath);
				mainFrame.getProgressBar().setValue(mainFrame.getProgressBar().getMaximum());//返回进度条的最大值
			} else {
				if(array == null){
					array = getBufferImages(mainFrame.getFilepath());
				}
				// 创建文件夹
				File newDirs = new File(mainFrame.getSavePath());
				newDirs.mkdirs();//创建多级目录
				completeCount = 0;
				//进度条的线程
				Runnable update = getUpdate(mainFrame.getProgressBar(), array.size());
				for (int i = 0; i < array.size(); i++) {
					++completeCount;
					//开启进度条
					new Thread(update).start();
					// 生成水印图片
					generate(array.get(i).getBuffer(), 
							savePath + Common.getNewFileorDirName(array.get(i).getFilename()));
				}
			}
			if(isDrag){
				isDrag = false;
				isSingle = true;
			}
			JOptionPane.showMessageDialog(null, "操作成功！");//弹框提示
		}else{
			JOptionPane.showMessageDialog(null, "日后添加新功能！");//弹框提示
		}
	}
	/**
	 * 
	 * @return 预览时的图片的绝对路径
	 */
	private String getFirstFilepath() {
		String firstFilepath = null;
		if (isSingle) {
			firstFilepath = mainFrame.getFilepath();
		} else {
			ArrayList<String> imageFilenames = Common.getImageFiles(mainFrame.getFilepath());
			if(!imageFilenames.isEmpty()){
				firstFilepath = imageFilenames.get(0);
			}
		}
		return firstFilepath;
	}


	/**
	 * @param filepath 图片的绝对路径
	 * @return 图像加文字之后的BufferedImage对象
	 */
	private BufferedImage addWatermark(String filepath) {
		Color fontColor = mainFrame.getFontcolor();
		String mark = mainFrame.getMark();
		int toward = mainFrame.getToward();
		Font font = mainFrame.getWaterMarkFont();
		float alpha = mainFrame.getWaterMarkAlpha();
		float scale = mainFrame.getScale();
		BufferedImage buffImg = ImageTool.watermark(filepath, font, fontColor,
				toward, mark, alpha, scale);
		return buffImg;
	}
	/**
	 * 生成添加文字后的图片
	 * @param buffImg 图像加文字之后的BufferedImage对象
	 * @param savePath 图像加文字之后的保存路径
	 */
	private void generate(BufferedImage buffImg, String savePath) {
		int temp = savePath.lastIndexOf(".") + 1;
		try {
			ImageIO.write(buffImg, savePath.substring(temp), new File(savePath));//写图片
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	/**
	 * 
	 * @return 选中的文件或文件夹的名字
	 */
	private String getSelectResult(){
		if (isSingle && style.equals(ButtonStyle.selectImage)) {
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			FileNameExtensionFilter filter = new FileNameExtensionFilter(//使用指定文件扩展名进行过滤
					"图片文件(*.bmp, *.gif, *.jpg, *.jpeg, *.png)", "bmp",
					"gif", "jpg", "jpeg", "png");
			fileChooser.setFileFilter(filter);
			fileChooser.setDialogTitle("选择图片文件");//设置显示到该过滤器的的标题
		}else{
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setDialogTitle("选择文件");
		}
		int result = fileChooser.showDialog(null, "确定");
		if (result == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile().getPath();
		}
		return null;
	}
	/**
	 * 
	 * @param progressBar 进度条
	 * @param maxValue 进度条的最大值
	 * @return 更新进度条的一个Runnable实例
	 */
	private Runnable getUpdate(final JProgressBar progressBar, int maxValue){
		progressBar.setMaximum(maxValue);//设置最大值
        Dimension d = progressBar.getSize();
        final Rectangle rect = new Rectangle(0,0, d.width, d.height);
        return new Runnable(){ 
			public void run() {
	            progressBar.setValue(completeCount);//查询该任务当前状态及使用返回值更新进度条
	            progressBar.paintImmediately(rect);//TODO
	        }  
	    };
	}
	/**
	 * 
	 * @param filepath
	 * @return 
	 */
	private ArrayList<FileBean> getBufferImages(String filepath){
		ArrayList<String> imageFilenames = Common.getImageFiles(filepath);
		if(imageFilenames.size() <= 0){
			JOptionPane.showMessageDialog(null, "找不到图片文件！");//弹框提示
			return null ;
		}
		ArrayList<FileBean> array = new ArrayList<FileBean>();
		for (int i = 0; i < imageFilenames.size(); i++) {
			String filename = imageFilenames.get(i);
			// 对图像加文字
			BufferedImage buffImg = addWatermark(imageFilenames.get(i));
			array.add(new FileBean(filename, buffImg));
		}
		return array;
	}
}
