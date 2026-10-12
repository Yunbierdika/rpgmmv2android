/*
 * LZString4Java By Rufus Huang 
 * https://github.com/rufushuang/lz-string4java
 * MIT License
 * 
 * Port from original JavaScript version by pieroxy 
 * https://github.com/pieroxy/lz-string
 */

package yunbierdika.rpgmmv2android.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LZString {

	private static char[] keyStrBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
	private static char[] keyStrUriSafe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$".toCharArray();
	private static Map<char[], Map<Character, Integer>> baseReverseDic = new HashMap<char[], Map<Character, Integer>>();

	private static char getBaseValue(char[] alphabet, Character character) {
		Map<Character, Integer> map = baseReverseDic.get(alphabet);
		if (map == null) {
			map = new HashMap<Character, Integer>();
			baseReverseDic.put(alphabet, map);
			for (int i = 0; i < alphabet.length; i++) {
				map.put(alphabet[i], i);
			}
		}
		return (char) map.get(character).intValue();
	}
	
	/**
	 * 【重要】这里刻意不再用 _compress(input, 6, ...) 的写法。
	 *
	 * RPG Maker MV 自带的 JS 版 lz-string（1.3.x）的 compressToBase64 是：
	 *   先 compress()（16 bit/字符）得到 UTF-16 串，再把这个串当成字节流按 3 字节一组转 Base64。
	 * 而 1.4.4 的写法（_compress(input, 6, ...) 再按 res.length()%4 补 '='）
	 * 两者「末尾补位」的方式不同，会让最后一个 Base64 字符不一致
	 * （实测：MV 那边输出 "...EAA"，1.4.4 输出 "...EA="），
	 * 于是 MV 解压时解出的字节数变成奇数、数据流不完整，
	 * decompressFromBase64 直接返回空字符串 —— 玩家表现就是
	 * 「存档放进去了，游戏里却像根本没放存档」。
	 *
	 * 所以这里严格按 1.3.x 的做法实现，保证与 RPG Maker MV 完全互通。
	 */
	public static String compressToBase64(String input) {
		if (input == null)
			return "";
		String res = LZString.compress(input);
		StringBuilder sb = new StringBuilder();
		int len = res.length();
		int i = 0;
		while (i < len * 2) {
			boolean has2, has3;
			int chr1, chr2 = 0, chr3 = 0;
			if (i % 2 == 0) {
				chr1 = res.charAt(i / 2) >> 8;
				chr2 = res.charAt(i / 2) & 255;
				has2 = true;
				if (i / 2 + 1 < len) {
					chr3 = res.charAt(i / 2 + 1) >> 8;
					has3 = true;
				} else {
					has3 = false;
				}
			} else {
				chr1 = res.charAt((i - 1) / 2) & 255;
				if ((i + 1) / 2 < len) {
					chr2 = res.charAt((i + 1) / 2) >> 8;
					chr3 = res.charAt((i + 1) / 2) & 255;
					has2 = true;
					has3 = true;
				} else {
					has2 = false;
					has3 = false;
				}
			}
			i += 3;

			int enc1 = chr1 >> 2;
			int enc2 = ((chr1 & 3) << 4) | (has2 ? (chr2 >> 4) : 0);
			int enc3 = has2 ? (((chr2 & 15) << 2) | (has3 ? (chr3 >> 6) : 0)) : 64;
			int enc4 = has3 ? (chr3 & 63) : 64;

			sb.append(keyStrBase64[enc1]).append(keyStrBase64[enc2])
			  .append(keyStrBase64[enc3]).append(keyStrBase64[enc4]);
		}
		return sb.toString();
	}
	
	public static String decompressFromBase64(final String inputStr) {
		if (inputStr == null)
			return "";
		if (inputStr.equals(""))
			return null;
		return LZString._decompress(inputStr.length(), 32, new DecompressFunctionWrapper() {
			@Override
			public char doFunc(int index) {
				return getBaseValue(keyStrBase64, inputStr.charAt(index));
			}
		});
	}	

	public static String compressToUTF16(String input) {
		if (input == null)
			return "";
		return LZString._compress(input, 15, new CompressFunctionWrapper() {
			@Override
			public char doFunc(int a) {
				return fc(a + 32);
			}
		}) + " ";
	}

	public static String decompressFromUTF16(final String compressedStr) {
		if (compressedStr == null)
			return "";
		if (compressedStr.isEmpty())
			return null;
		return LZString._decompress(compressedStr.length(), 16384, new DecompressFunctionWrapper() {
			@Override
			public char doFunc(int index) {
				return (char) (compressedStr.charAt(index) - 32);
			}
		});
	}
	
	//TODO: java has no Uint8Array type, what can we do?
	
	public static String compressToEncodedURIComponent(String input) {
		if (input == null)
			return "";
		return LZString._compress(input, 6, new CompressFunctionWrapper() {
			@Override
			public char doFunc(int a) {
				return keyStrUriSafe[a];
			}
		});
	}

	public static String decompressFromEncodedURIComponent(String inputStr) {
	    if (inputStr == null) return "";
	    if (inputStr.isEmpty()) return null;
	    final String urlEncodedInputStr = inputStr.replace(' ', '+');
		return LZString._decompress(urlEncodedInputStr.length(), 32, new DecompressFunctionWrapper() {
			@Override
			public char doFunc(int index) {
				return getBaseValue(keyStrUriSafe, urlEncodedInputStr.charAt(index));
			}
		});
	}

	private static abstract class CompressFunctionWrapper {
		public abstract char doFunc(int i);
	}

	public static String compress(String uncompressed) {
		return LZString._compress(uncompressed, 16, new CompressFunctionWrapper() {
			@Override
			public char doFunc(int a) {
				return fc(a);
			}
		});
	}
	private static String _compress(String uncompressedStr, int bitsPerChar, CompressFunctionWrapper getCharFromInt) {
	    if (uncompressedStr == null) return "";
		int i, value;
		Map<String, Integer> context_dictionary = new HashMap<String, Integer>();
		Set<String> context_dictionaryToCreate = new HashSet<String>();
		String context_c = "";
		String context_wc = "";
		String context_w = "";
		int context_enlargeIn = 2; // Compensate for the first entry which should not count
		int context_dictSize = 3;
		int context_numBits = 2;
		StringBuilder context_data = new StringBuilder(uncompressedStr.length() / 3);
		int context_data_val = 0;
		int context_data_position = 0;
		int ii;
		
		for (ii = 0; ii < uncompressedStr.length(); ii += 1) {
			context_c = String.valueOf(uncompressedStr.charAt(ii));
			if (!context_dictionary.containsKey(context_c)) {
				context_dictionary.put(context_c, context_dictSize++);
				context_dictionaryToCreate.add(context_c);
			}

			context_wc = context_w + context_c;
			if (context_dictionary.containsKey(context_wc)) {
				context_w = context_wc;
			} else {
				if (context_dictionaryToCreate.contains(context_w)) {
					if (context_w.charAt(0) < 256) {
						for (i = 0; i < context_numBits; i++) {
							context_data_val = (context_data_val << 1);
							if (context_data_position == bitsPerChar - 1) {
								context_data_position = 0;
								context_data.append(getCharFromInt.doFunc(context_data_val));
								context_data_val = 0;
							} else {
								context_data_position++;
							}
						}
						value = context_w.charAt(0);
						for (i = 0; i < 8; i++) {
							context_data_val = (context_data_val << 1) | (value & 1);
							if (context_data_position == bitsPerChar - 1) {
								context_data_position = 0;
								context_data.append(getCharFromInt.doFunc(context_data_val));
								context_data_val = 0;
							} else {
								context_data_position++;
							}
							value = value >> 1;
						}
					} else {
						value = 1;
						for (i = 0; i < context_numBits; i++) {
							context_data_val = (context_data_val << 1) | value;
							if (context_data_position == bitsPerChar - 1) {
								context_data_position = 0;
								context_data.append(getCharFromInt.doFunc(context_data_val));
								context_data_val = 0;
							} else {
								context_data_position++;
							}
							value = 0;
						}
						value = context_w.charAt(0);
						for (i = 0; i < 16; i++) {
							context_data_val = (context_data_val << 1) | (value & 1);
							if (context_data_position == bitsPerChar - 1) {
								context_data_position = 0;
								context_data.append(getCharFromInt.doFunc(context_data_val));
								context_data_val = 0;
							} else {
								context_data_position++;
							}
							value = value >> 1;
						}
					}
					context_enlargeIn--;
					if (context_enlargeIn == 0) {
						context_enlargeIn = powerOf2(context_numBits);
						context_numBits++;
					}
					context_dictionaryToCreate.remove(context_w);
				} else {
					value = context_dictionary.get(context_w);
					for (i = 0; i < context_numBits; i++) {
						context_data_val = (context_data_val << 1) | (value & 1);
						if (context_data_position == bitsPerChar - 1) {
							context_data_position = 0;
							context_data.append(getCharFromInt.doFunc(context_data_val));
							context_data_val = 0;
						} else {
							context_data_position++;
						}
						value = value >> 1;
					}

				}
				context_enlargeIn--;
				if (context_enlargeIn == 0) {
					context_enlargeIn = powerOf2(context_numBits);
					context_numBits++;
				}
				// Add wc to the dictionary.
				context_dictionary.put(context_wc, context_dictSize++);
				context_w = context_c;
			}
		}
		
		// Output the code for w.
		if (!context_w.isEmpty()) {
			if (context_dictionaryToCreate.contains(context_w)) {
				if (context_w.charAt(0) < 256) {
					for (i = 0; i < context_numBits; i++) {
						context_data_val = (context_data_val << 1);
						if (context_data_position == bitsPerChar - 1) {
							context_data_position = 0;
							context_data.append(getCharFromInt.doFunc(context_data_val));
							context_data_val = 0;
						} else {
							context_data_position++;
						}
					}
					value = context_w.charAt(0);
					for (i = 0; i < 8; i++) {
						context_data_val = (context_data_val << 1) | (value & 1);
						if (context_data_position == bitsPerChar - 1) {
							context_data_position = 0;
							context_data.append(getCharFromInt.doFunc(context_data_val));
							context_data_val = 0;
						} else {
							context_data_position++;
						}
						value = value >> 1;
					}
				} else {
					value = 1;
					for (i = 0; i < context_numBits; i++) {
						context_data_val = (context_data_val << 1) | value;
						if (context_data_position == bitsPerChar - 1) {
							context_data_position = 0;
							context_data.append(getCharFromInt.doFunc(context_data_val));
							context_data_val = 0;
						} else {
							context_data_position++;
						}
						value = 0;
					}
					value = context_w.charAt(0);
					for (i = 0; i < 16; i++) {
						context_data_val = (context_data_val << 1) | (value & 1);
						if (context_data_position == bitsPerChar - 1) {
							context_data_position = 0;
							context_data.append(getCharFromInt.doFunc(context_data_val));
							context_data_val = 0;
						} else {
							context_data_position++;
						}
						value = value >> 1;
					}
				}
				context_enlargeIn--;
				if (context_enlargeIn == 0) {
					context_enlargeIn = powerOf2(context_numBits);
					context_numBits++;
				}
				context_dictionaryToCreate.remove(context_w);
			} else {
				value = context_dictionary.get(context_w);
				for (i = 0; i < context_numBits; i++) {
					context_data_val = (context_data_val << 1) | (value & 1);
					if (context_data_position == bitsPerChar - 1) {
						context_data_position = 0;
						context_data.append(getCharFromInt.doFunc(context_data_val));
						context_data_val = 0;
					} else {
						context_data_position++;
					}
					value = value >> 1;
				}

			}
			context_enlargeIn--;
			if (context_enlargeIn == 0) {
				context_enlargeIn = powerOf2(context_numBits);
				context_numBits++;
			}
		}

		// Mark the end of the stream
		value = 2;
		for (i = 0; i < context_numBits; i++) {
			context_data_val = (context_data_val << 1) | (value & 1);
			if (context_data_position == bitsPerChar - 1) {
				context_data_position = 0;
				context_data.append(getCharFromInt.doFunc(context_data_val));
				context_data_val = 0;
			} else {
				context_data_position++;
			}
			value = value >> 1;
		}

		// Flush the last char
		while (true) {
			context_data_val = (context_data_val << 1);
			if (context_data_position == bitsPerChar - 1) {
				context_data.append(getCharFromInt.doFunc(context_data_val));
				break;
			}
			else
				context_data_position++;
		}
		return context_data.toString();
	}
	
	private static abstract class DecompressFunctionWrapper {
		public abstract char doFunc(int i);
	}
	protected static class DecData {
		public char val;
		public int position;
		public int index;		
	}

	public static String f(int i) {
		return String.valueOf((char) i);
	}
	public static char fc(int i) {
		return (char) i;
	}

	public static String decompress(final String compressed) {
		if (compressed == null)
			return "";
		if (compressed.isEmpty())
			return null;
		return LZString._decompress(compressed.length(), 32768, new DecompressFunctionWrapper() {
			@Override
			public char doFunc(int i) {
				return compressed.charAt(i);
			}
		});
	}
	private static String _decompress(int length, int resetValue, DecompressFunctionWrapper getNextValue) {
		List<String> dictionary = new ArrayList<String>();
		// TODO: is next an unused variable in original lz-string?
		@SuppressWarnings("unused")
		int next;
		int enlargeIn = 4;
		int dictSize = 4;
		int numBits = 3;
		String entry = "";
		StringBuilder result = new StringBuilder();
		String w;
		int bits, resb; int maxpower, power;
		String c = null;
		DecData data = new DecData();
		data.val = getNextValue.doFunc(0);
		data.position = resetValue;
		data.index = 1;
		
		for (int i = 0; i < 3; i += 1) {
			dictionary.add(i, f(i));
		}
		
		bits = 0;
		maxpower = (int) powerOf2(2);
		power = 1;
		while (power != maxpower) {
			resb = data.val & data.position;
			data.position >>= 1;
			if (data.position == 0) {
				data.position = resetValue;
				data.val = getNextValue.doFunc(data.index++);
			}
			bits |= (resb > 0 ? 1 : 0) * power;
			power <<= 1;
		}
		
	    switch (next = bits) {
	      case 0:
	          bits = 0;
	          maxpower = (int) powerOf2(8);
	          power=1;
	          while (power != maxpower) {
	            resb = data.val & data.position;
	            data.position >>= 1;
	            if (data.position == 0) {
	              data.position = resetValue;
	              data.val = getNextValue.doFunc(data.index++);
	            }
	            bits |= (resb>0 ? 1 : 0) * power;
	            power <<= 1;
	          }
	        c = f(bits);
	        break;
	      case 1:
	          bits = 0;
	          maxpower = powerOf2(16);
	          power=1;
	          while (power!=maxpower) {
	            resb = data.val & data.position;
	            data.position >>= 1;
	            if (data.position == 0) {
	              data.position = resetValue;
	              data.val = getNextValue.doFunc(data.index++);
	            }
	            bits |= (resb>0 ? 1 : 0) * power;
	            power <<= 1;
	          }
	        c = f(bits);
	        break;
	      case 2:
	        return "";
	    }
	    dictionary.add(3, c);
	    w = c;
		result.append(w);
	    while (true) {
	        if (data.index > length) {
	          return "";
	        }

	        bits = 0;
	        maxpower = powerOf2(numBits);
	        power=1;
	        while (power!=maxpower) {
	          resb = data.val & data.position;
	          data.position >>= 1;
	          if (data.position == 0) {
	            data.position = resetValue;
	            data.val = getNextValue.doFunc(data.index++);
	          }
	          bits |= (resb>0 ? 1 : 0) * power;
	          power <<= 1;
	        }
	        // TODO: very strange here, c above is as char/string, here further is a int, rename "c" in the switch as "cc"
	        int cc;
	        switch (cc = bits) {
	          case 0:
	            bits = 0;
	            maxpower = powerOf2(8);
	            power=1;
	            while (power!=maxpower) {
	              resb = data.val & data.position;
	              data.position >>= 1;
	              if (data.position == 0) {
	                data.position = resetValue;
	                data.val = getNextValue.doFunc(data.index++);
	              }
	              bits |= (resb>0 ? 1 : 0) * power;
	              power <<= 1;
	            }

	            dictionary.add(dictSize++, f(bits));
	            cc = dictSize-1;
	            enlargeIn--;
	            break;
	          case 1:
	            bits = 0;
	            maxpower = powerOf2(16);
	            power=1;
	            while (power!=maxpower) {
	              resb = data.val & data.position;
	              data.position >>= 1;
	              if (data.position == 0) {
	                data.position = resetValue;
	                data.val = getNextValue.doFunc(data.index++);
	              }
	              bits |= (resb>0 ? 1 : 0) * power;
	              power <<= 1;
	            }
	            dictionary.add(dictSize++, f(bits));
	            cc = dictSize-1;
	            enlargeIn--;
	            break;
	          case 2:
	        	return result.toString();
			}

	        if (enlargeIn == 0) {
	          enlargeIn = powerOf2(numBits);
	          numBits++;
	        }

	        if (cc < dictionary.size() && dictionary.get(cc) != null) {
	        	entry = dictionary.get(cc);
	        } else {
	          if (cc == dictSize) {
	            entry = w + w.charAt(0);
	          } else {
	            return null;
	          }
	        }
	        result.append(entry);

	        // Add w+entry[0] to the dictionary.
	        dictionary.add(dictSize++, w + entry.charAt(0));
	        enlargeIn--;

	        w = entry;

	        if (enlargeIn == 0) {
	          enlargeIn = powerOf2(numBits);
	          numBits++;
	        }

	      }
		
	}

	private static int powerOf2(int power) {
	    return 1 << power;
    }
	
	public static void main(String[] args) {
		String input;
//		input = "hello";
		input = "hello1hello2hello3hello4hello5hello6hello7hello8hello9helloAhelloBhelloChelloDhelloEhelloF";
	
		System.out.println(decompress(compress(input)));
		System.out.println(decompressFromBase64(compressToBase64(input)));
		System.out.println(decompressFromUTF16(compressToUTF16(input)));
		System.out.println(decompressFromEncodedURIComponent(compressToEncodedURIComponent(input)));
	}
}
