
# mediview by wojkuzb
Aplikacja do wyświetlania plików DICOM.

Napisana w języku Kotlin, wykorzystuje Compose multiplatform do GUI, korutyny do wielowątkowości, i bibliotekę twelveMonkeys do wczytywania obrazów.

Oficjalna nazwa projektu to MediView, ale jako żart okno pokazuje nazwę "feetpic"

⚠ Ponieważ aplikacja operuje na obrazach, proszę zwiększyć ilość pamięci
dostępnej dla aplikacji do minimum:
    `(ilość obrazków jednego skanu) * 1 MB`
Czyli ilość plików dicom razy 1 MB.
Robi się to dodając np. ` -Xmx1024m` do konfiguracji 'VM options'.
Za 1024 wstawić tu należy obliczoną wartość, jeśli 1024 MB nie wystarczają.
