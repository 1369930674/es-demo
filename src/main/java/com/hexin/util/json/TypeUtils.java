package com.hexin.util.json;

import org.apache.commons.codec.binary.Base64;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author yf
 */
public class TypeUtils {
    public TypeUtils() {
    }

    public static String castToString(Object value) {
        return value == null ? null : value.toString();
    }

    public static Byte castToByte(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Number) {
            return ((Number)value).byteValue();
        } else if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.length() == 0) {
                return null;
            } else {
                return !"null".equals(strVal) && !"NULL".equals(strVal) ? Byte.parseByte(strVal) : null;
            }
        } else {
            throw new RuntimeException("can not cast to byte, value : " + value);
        }
    }

    public static Character castToChar(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Character) {
            return (Character)value;
        } else if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.length() == 0) {
                return null;
            } else if (strVal.length() != 1) {
                throw new RuntimeException("can not cast to byte, value : " + value);
            } else {
                return strVal.charAt(0);
            }
        } else {
            throw new RuntimeException("can not cast to byte, value : " + value);
        }
    }

    public static Short castToShort(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Number) {
            return ((Number)value).shortValue();
        } else if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.length() == 0) {
                return null;
            } else {
                return !"null".equals(strVal) && !"NULL".equals(strVal) ? Short.parseShort(strVal) : null;
            }
        } else {
            throw new RuntimeException("can not cast to short, value : " + value);
        }
    }

    public static BigDecimal castToBigDecimal(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof BigDecimal) {
            return (BigDecimal)value;
        } else if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger)value);
        } else {
            String strVal = value.toString();
            return strVal.length() == 0 ? null : new BigDecimal(strVal);
        }
    }

    public static BigInteger castToBigInteger(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof BigInteger) {
            return (BigInteger)value;
        } else if (!(value instanceof Float) && !(value instanceof Double)) {
            String strVal = value.toString();
            return strVal.length() == 0 ? null : new BigInteger(strVal);
        } else {
            return BigInteger.valueOf(((Number)value).longValue());
        }
    }

    public static Float castToFloat(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Number) {
            return ((Number)value).floatValue();
        } else if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.length() == 0) {
                return null;
            } else {
                return !"null".equals(strVal) && !"NULL".equals(strVal) ? Float.parseFloat(strVal) : null;
            }
        } else {
            throw new RuntimeException("can not cast to float, value : " + value);
        }
    }

    public static Double castToDouble(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Number) {
            return ((Number)value).doubleValue();
        } else if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.length() == 0) {
                return null;
            } else {
                return !"null".equals(strVal) && !"NULL".equals(strVal) ? Double.parseDouble(strVal) : null;
            }
        } else {
            throw new RuntimeException("can not cast to double, value : " + value);
        }
    }

    public static Date castToDate(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Calendar) {
            return ((Calendar)value).getTime();
        } else if (value instanceof Date) {
            return (Date)value;
        } else {
            long longValue = -1L;
            if (value instanceof Number) {
                longValue = ((Number)value).longValue();
                return new Date(longValue);
            } else {
                if (value instanceof String) {
                    String strVal = (String)value;
                    if (strVal.indexOf(45) != -1) {
                        String format;
                        if (strVal.length() == 10) {
                            format = "yyyy-MM-dd";
                        } else if (strVal.length() == "yyyy-MM-dd HH:mm:ss".length()) {
                            format = "yyyy-MM-dd HH:mm:ss";
                        } else {
                            format = "yyyy-MM-dd HH:mm:ss.SSS";
                        }

                        SimpleDateFormat dateFormat = new SimpleDateFormat(format);

                        try {
                            return dateFormat.parse(strVal);
                        } catch (ParseException var7) {
                            throw new RuntimeException("can not cast to Date, value : " + strVal);
                        }
                    }

                    if (strVal.length() == 0) {
                        return null;
                    }

                    longValue = Long.parseLong(strVal);
                }

                if (longValue < 0L) {
                    throw new RuntimeException("can not cast to Date, value : " + value);
                } else {
                    return new Date(longValue);
                }
            }
        }
    }

    public static java.sql.Date castToSqlDate(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Calendar) {
            return new java.sql.Date(((Calendar)value).getTimeInMillis());
        } else if (value instanceof java.sql.Date) {
            return (java.sql.Date)value;
        } else if (value instanceof Date) {
            return new java.sql.Date(((Date)value).getTime());
        } else {
            long longValue = 0L;
            if (value instanceof Number) {
                longValue = ((Number)value).longValue();
            }

            if (value instanceof String) {
                String strVal = (String)value;
                if (strVal.length() == 0) {
                    return null;
                }

                longValue = Long.parseLong(strVal);
            }

            if (longValue <= 0L) {
                throw new RuntimeException("can not cast to Date, value : " + value);
            } else {
                return new java.sql.Date(longValue);
            }
        }
    }

    public static Timestamp castToTimestamp(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Calendar) {
            return new Timestamp(((Calendar)value).getTimeInMillis());
        } else if (value instanceof Timestamp) {
            return (Timestamp)value;
        } else if (value instanceof Date) {
            return new Timestamp(((Date)value).getTime());
        } else {
            long longValue = 0L;
            if (value instanceof Number) {
                longValue = ((Number)value).longValue();
            }

            if (value instanceof String) {
                String strVal = (String)value;
                if (strVal.length() == 0) {
                    return null;
                }

                longValue = Long.parseLong(strVal);
            }

            if (longValue <= 0L) {
                throw new RuntimeException("can not cast to Date, value : " + value);
            } else {
                return new Timestamp(longValue);
            }
        }
    }

    public static Long castToLong(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Number) {
            return ((Number)value).longValue();
        } else {
            if (value instanceof String) {
                String strVal = (String)value;
                if (strVal.length() == 0) {
                    return null;
                }

                if ("null".equals(strVal) || "NULL".equals(strVal)) {
                    return null;
                }

                try {
                    return Long.parseLong(strVal);
                } catch (NumberFormatException var4) {
                    var4.printStackTrace();
                }
            }

            throw new RuntimeException("can not cast to long, value : " + value);
        }
    }

    public static Integer castToInt(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            return (Integer)value;
        } else if (value instanceof Number) {
            return ((Number)value).intValue();
        } else if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.length() == 0) {
                return null;
            } else if ("null".equals(strVal)) {
                return null;
            } else {
                return !"null".equals(strVal) && !"NULL".equals(strVal) ? Integer.parseInt(strVal) : null;
            }
        } else {
            throw new RuntimeException("can not cast to int, value : " + value);
        }
    }

    public static byte[] castToBytes(Object value) {
        if (value instanceof byte[]) {
            return (byte[])value;
        } else if (value instanceof String) {
            return Base64.decodeBase64((String)value);
        } else {
            throw new RuntimeException("can not cast to int, value : " + value);
        }
    }

    public static Boolean castToBoolean(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            return (Boolean)value;
        } else if (value instanceof Number) {
            return ((Number)value).intValue() == 1;
        } else {
            if (value instanceof String) {
                String strVal = (String)value;
                if (strVal.length() == 0) {
                    return null;
                }

                if ("true".equalsIgnoreCase(strVal)) {
                    return Boolean.TRUE;
                }

                if ("false".equalsIgnoreCase(strVal)) {
                    return Boolean.FALSE;
                }

                if ("1".equals(strVal)) {
                    return Boolean.TRUE;
                }

                if ("0".equals(strVal)) {
                    return Boolean.FALSE;
                }

                if ("null".equals(strVal) || "NULL".equals(strVal)) {
                    return null;
                }
            }

            throw new RuntimeException("can not cast to boolean, value : " + value);
        }
    }
}
