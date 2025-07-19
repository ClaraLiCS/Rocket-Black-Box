import sys
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation
from scipy.signal import savgol_filter

def read_alturas_stdin():
    alturas = []
    for line in sys.stdin:
        line = line.strip()
        if line:
            try:
                alturas.append(float(line))
            except ValueError:
                continue
    return alturas

def suavizar_datos(y, window=11, poly=3):
    if len(y) < window:
        return y
    return savgol_filter(y, window_length=window, polyorder=poly)

def main():
    alturas = read_alturas_stdin()
    if not alturas:
        print("No se recibieron datos.")
        return

    alturas = np.array(alturas)
    alturas_suave = suavizar_datos(alturas)

    tiempos = np.arange(len(alturas)) * 0.1  # 100ms -> 0.1s entre muestras

    fig, ax = plt.subplots()
    line, = ax.plot([], [], lw=2, color='blue')
    rocket, = ax.plot([], [], marker='^', markersize=15, color='red') 
    ax.set_xlim(tiempos[0], tiempos[-1])
    ax.set_ylim(min(alturas_suave) - 5, max(alturas_suave) + 5)
    ax.set_xlabel('Tiempo (s)')
    ax.set_ylabel('Altura (m)')
    ax.set_title('Simulación de Vuelo del Cohete')

    xdata, ydata = [], []

    def update(frame):
        xdata.append(tiempos[frame])
        ydata.append(alturas_suave[frame])
        line.set_data(xdata, ydata)
        return line, rocket,

    ani = animation.FuncAnimation(
        fig,
        update,
        frames=len(tiempos),
        interval=100,  # 100 ms por punto
        blit=True,
        repeat=False
    )

    plt.tight_layout()
    plt.show()

if __name__ == "__main__":
    main()
