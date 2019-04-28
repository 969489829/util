package test;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import test.AppUtil.1;

public class AppUtil {
	private static String patchPath = "../patch/";
	private static String completePath = "./";
	private static final String FILE_LIST = "/file_list";

	public static void makeUpdateVersion(String oldFilePath, String newFilePath, String makeFilePath, String fileName)
			throws Exception {
		HashMap oldMap = null;
		ZipFile newFile = null;
		HashMap newMap = null;

		try {
			ZipFile oldFile = new ZipFile(new File(oldFilePath));
			oldMap = getMap(oldFile);
		} catch (Exception arg11) {
			System.out.println("打开压缩文件：" + oldFilePath + "错误，可能是文件格式问题，请重新压缩再试");
			throw new RuntimeException(arg11);
		}

		try {
			newFile = new ZipFile(new File(newFilePath));
			newMap = getMap(newFile);
		} catch (Exception arg10) {
			System.out.println("打开压缩文件：" + newFilePath + "错误，可能是文件格式问题，请重新压缩再试");
			throw new RuntimeException(arg10);
		}

		HashMap changeMap = getChange(oldMap, newMap);

		try {
			makeFile(newFile, changeMap, newMap, makeFilePath, fileName);
		} catch (Exception arg9) {
			System.out.println("生成校验文件异常");
			throw new RuntimeException(arg9);
		}
	}

	private static HashMap<String, String> getMap(ZipFile file) {
		InputStream inputStream = null;
		BufferedInputStream bufferedInputStream = null;
		Enumeration entries = file.entries();
		byte[] arr = new byte[1024];
		HashMap hashMap = new HashMap();

		try {
			while (entries.hasMoreElements()) {
				MessageDigest e = MessageDigest.getInstance("MD5");
				ZipEntry nextElement = (ZipEntry) entries.nextElement();
				inputStream = file.getInputStream(nextElement);
				bufferedInputStream = new BufferedInputStream(inputStream);

				int len;
				while ((len = bufferedInputStream.read(arr)) != -1) {
					e.update(arr, 0, len);
				}

				if (!nextElement.isDirectory()) {
					hashMap.put(nextElement.getName(), bytesToHexString(e.digest()));
				}
			}
		} catch (Exception arg11) {
			System.out.println("md5加密异常");
			throw new RuntimeException(arg11);
		} finally {
			close(new Closeable[]{bufferedInputStream, inputStream});
		}

		return hashMap;
	}

	private static HashMap<String, String> getChange(HashMap<String, String> oldMap, HashMap<String, String> newMap) {
		HashMap hashMap = new HashMap();
		newMap.forEach((k, v) -> {
			String i = (String) oldMap.get(k);
			if (i == null || !v.equals(i)) {
				hashMap.put(k, v);
			}

		});
		return hashMap;
	}

	private static void makeFile(ZipFile file, HashMap<String, String> changeMap, HashMap<String, String> newMap,
			String path, String fileName) {
		FileOutputStream fileOutputStream = null;
		ZipOutputStream zipOutputStream = null;

		try {
			Enumeration e = file.entries();
			File file2 = new File(path);
			if (!file2.exists()) {
				file2.mkdirs();
			}

			File aimFile = new File(path + "/" + fileName);
			fileOutputStream = new FileOutputStream(aimFile);
			zipOutputStream = new ZipOutputStream(fileOutputStream);
			byte[] arr = new byte[1024];

			while (true) {
				ZipEntry entrySet;
				do {
					do {
						if (!e.hasMoreElements()) {
							zipOutputStream.putNextEntry(new ZipEntry("/file_list"));
							Set entrySet1 = newMap.entrySet();
							zipOutputStream.write("{".getBytes());
							Iterator iterator1 = entrySet1.iterator();

							while (iterator1.hasNext()) {
								Entry next1 = (Entry) iterator1.next();
								zipOutputStream.write(
										("\"/" + (String) next1.getKey() + "\":\"" + (String) next1.getValue() + "\"")
												.getBytes());
								if (iterator1.hasNext()) {
									zipOutputStream.write(",".getBytes());
								}
							}

							zipOutputStream.write("}".getBytes());
							return;
						}

						entrySet = (ZipEntry) e.nextElement();
					} while (changeMap.get(entrySet.getName()) == null);

					zipOutputStream.putNextEntry(new ZipEntry(entrySet.getName()));
				} while (changeMap.get(entrySet.getName()) == null);

				System.out.println("拷贝文件:" + entrySet.getName());
				InputStream iterator = file.getInputStream(entrySet);
				BufferedInputStream next = new BufferedInputStream(iterator);

				int len;
				while ((len = next.read(arr)) != -1) {
					zipOutputStream.write(arr, 0, len);
				}

				zipOutputStream.closeEntry();
			}
		} catch (Exception arg17) {
			System.out.println("压缩文件异常");
			throw new RuntimeException(arg17);
		} finally {
			close(new Closeable[]{zipOutputStream, fileOutputStream, file});
		}
	}

	public static void close(Closeable... ios) {
		Closeable[] arg3 = ios;
		int arg2 = ios.length;

		for (int arg1 = 0; arg1 < arg2; ++arg1) {
			Closeable closeable = arg3[arg1];
			if (closeable != null) {
				try {
					closeable.close();
				} catch (IOException arg5) {
					System.out.println("关闭流异常");
				}
			}
		}

	}

	private static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src != null && src.length > 0) {
			for (int i = 0; i < src.length; ++i) {
				int v = src[i] & 255;
				String hv = Integer.toHexString(v);
				if (hv.length() < 2) {
					stringBuilder.append(0);
				}

				stringBuilder.append(hv);
			}

			return stringBuilder.toString();
		} else {
			return null;
		}
	}

	public static void createIfNotExists(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}

	}

	public static String getNewVersionName() {
		File file = new File(completePath);
		String[] files = file.list(getFilenameFilter());
		Arrays.sort(files);
		return files[files.length - 1];
	}

	public static String getFileVersion(String name) {
		return name.replaceAll("app_", "").replaceAll(".zip", "");
	}

	public static FilenameFilter getFilenameFilter() {
      return new 1();
   }

	public static void deletePatchFiles() {
		File file = new File(patchPath);
		File[] listFiles = file.listFiles();
		if (listFiles != null && listFiles.length > 0) {
			System.out.println("删除过时补丁包");
			File[] arg4 = listFiles;
			int arg3 = listFiles.length;

			for (int arg2 = 0; arg2 < arg3; ++arg2) {
				File file3 = arg4[arg2];
				file3.delete();
			}
		}

	}

	public static void updateAllVersions() throws Exception {
		String newFilePath = getNewVersionName();
		System.out.println("发布版本[" + newFilePath + "]");
		File file = new File(completePath);
		String[] list = file.list(getFilenameFilter());
		String newVersion = getFileVersion(newFilePath);
		String[] arg6 = list;
		int arg5 = list.length;

		for (int arg4 = 0; arg4 < arg5; ++arg4) {
			String oldfile = arg6[arg4];
			if (!oldfile.equals(newFilePath)) {
				System.out.println("开始生成[" + oldfile + "]的增量包");
				String oldVersion = getFileVersion(oldfile);
				String patchname = oldVersion + "-" + newVersion + ".zip";
				makeUpdateVersion(oldfile, newFilePath, patchPath, patchname);
			}
		}

	}

	public static void main(String[] args) throws Exception {
		deletePatchFiles();
		updateAllVersions();
	}
}