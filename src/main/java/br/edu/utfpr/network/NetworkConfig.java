package br.edu.utfpr.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class NetworkConfig {

    public static final String IP_PADRAO = "127.0.0.1";
    public static final int PORTA_PADRAO = 8080;

    private NetworkConfig() {
    }

    public static String resolverIpServidor(String[] args) {
        if (args != null && args.length >= 1 && !args[0].isBlank()) {
            return args[0].trim();
        }

        String ipAmbiente = System.getenv("SERVIDOR_IP");
        if (ipAmbiente != null && !ipAmbiente.isBlank()) {
            return ipAmbiente.trim();
        }

        String ipPropriedade = System.getProperty("servidor.ip");
        if (ipPropriedade != null && !ipPropriedade.isBlank()) {
            return ipPropriedade.trim();
        }

        return IP_PADRAO;
    }

    public static int resolverPortaServidor(String[] args) {
        if (args != null && args.length >= 2 && !args[1].isBlank()) {
            return Integer.parseInt(args[1].trim());
        }

        String portaAmbiente = System.getenv("SERVIDOR_PORTA");
        if (portaAmbiente != null && !portaAmbiente.isBlank()) {
            return Integer.parseInt(portaAmbiente.trim());
        }

        String portaPropriedade = System.getProperty("servidor.porta");
        if (portaPropriedade != null && !portaPropriedade.isBlank()) {
            return Integer.parseInt(portaPropriedade.trim());
        }

        return PORTA_PADRAO;
    }

    public static boolean ipFoiConfiguradoExternamente(String[] args) {
        if (args != null && args.length >= 1 && !args[0].isBlank()) {
            return true;
        }
        String ipAmbiente = System.getenv("SERVIDOR_IP");
        if (ipAmbiente != null && !ipAmbiente.isBlank()) {
            return true;
        }
        String ipPropriedade = System.getProperty("servidor.ip");
        return ipPropriedade != null && !ipPropriedade.isBlank();
    }

    public static boolean portaFoiConfiguradaExternamente(String[] args) {
        if (args != null && args.length >= 2 && !args[1].isBlank()) {
            return true;
        }
        String portaAmbiente = System.getenv("SERVIDOR_PORTA");
        if (portaAmbiente != null && !portaAmbiente.isBlank()) {
            return true;
        }
        String portaPropriedade = System.getProperty("servidor.porta");
        return portaPropriedade != null && !portaPropriedade.isBlank();
    }

    public static boolean isServidorLocal(String ipServidor) {
        if (ipServidor == null || ipServidor.isBlank()) {
            return true;
        }

        String ip = ipServidor.trim().toLowerCase();
        return ip.equals("127.0.0.1")
                || ip.equals("localhost")
                || ip.equals("0.0.0.0")
                || ip.equals("::1");
    }

    public static List<String> listarIpsLocais() {
        List<String> ips = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> enderecos = iface.getInetAddresses();
                while (enderecos.hasMoreElements()) {
                    InetAddress endereco = enderecos.nextElement();
                    if (endereco instanceof Inet4Address && !endereco.isLoopbackAddress()) {
                        ips.add(endereco.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
            // Mantem a lista vazia; o servidor ainda funciona em localhost.
        }

        return ips;
    }
}
